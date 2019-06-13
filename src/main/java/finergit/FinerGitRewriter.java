package finergit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import finergit.ast.FinerJavaFileBuilder;
import finergit.ast.FinerJavaModule;
import finergit.util.RevCommitUtil;
import jp.ac.titech.c.se.stein.core.ConcurrentRepositoryRewriter;
import jp.ac.titech.c.se.stein.core.EntrySet;
import jp.ac.titech.c.se.stein.core.EntrySet.Entry;
import jp.ac.titech.c.se.stein.core.EntrySet.EntryList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.nio.charset.Charset;

public class FinerGitRewriter extends ConcurrentRepositoryRewriter {

    private static final Logger log = LoggerFactory.getLogger(FinerGitRewriter.class);
    private final FinerGitConfig config;
    private final FinerJavaFileBuilder builder;
    public static Map<String, String> codeMapping = new HashMap<>();
    //<Javaのコード,Javapコード> １つの.javaから複数の.classができてるときは、javapコードを全部１つにまとめてる
    private static boolean isFirst; //無理やり2回回してるのなんとかしたいけど
    private int commitNumber = 0;

    public FinerGitRewriter(final FinerGitConfig config, final boolean isFirst) {
        this.config = config;
        this.builder = new FinerJavaFileBuilder(config);
        setConcurrent(config.isParallel());
        setPathSensitive(true);
        this.isFirst = isFirst;
    }

    @Override
    protected String rewriteCommitMessage(final String message, final RevCommit commit) {
        return "<OriginalCommitID:" + RevCommitUtil.getAbbreviatedID(commit) + "> " + message;
    }

    @Override
    public EntrySet rewriteEntry(final Entry entry) {
        if (entry.isTree()) {
            return super.rewriteEntry(entry);
        }

        // Treats non-java files
        if (!entry.name.endsWith(".java")) {
            return config.isOtherFilesIncluded() ? super.rewriteEntry(entry) : Entry.EMPTY;
        }

        // Convert to finer modules
        final EntryList result = new EntryList();
        if (config.isOriginalJavaIncluded()) {
            log.debug("Keep original file: {}", entry);
            result.add(entry);
        }
        /* メソッド切り出し */
    /*
    for (final FinerJavaModule m : extractFinerModules(entry)) {
      final String finerSource = // 最終行に改行を入れないと途中行とのマッチングが正しく行われない
          String.join(System.lineSeparator(), m.getLines()) + System.lineSeparator();
      final ObjectId newId = writeBlob(finerSource.getBytes(StandardCharsets.UTF_8));
      final String name = m.getFileName();
      log.debug("Generate finer module: {} -> {} {}", entry, name, newId.name());
      result.add(new Entry(entry.mode, name, newId, entry.pathContext));
    }
   */
        /* メソッド1行にしたファイルの作成 */
    /*
    final String base = entry.pathContext + "/" + entry.name;
    final String source = new String(readBlob(entry.id), StandardCharsets.UTF_8);
    final String txt=builder.getFinerJavaString(base,source);
    final ObjectId newId = writeBlob(txt.getBytes(StandardCharsets.UTF_8));
    result.add(new Entry(entry.mode, entry.name, newId, entry.pathContext));
    */

        //1回目の実行時ならなにもしない
        if (isFirst) return result;

        //Mapのソースと一致したらエントリに追加
        //ファイル名は.javaのまんまに　ファイルの場所も.javaのそのまま
        final String source = new String(readBlob(entry.id), StandardCharsets.UTF_8);
        if (codeMapping.containsKey(source)) {
            final ObjectId newId = writeBlob(codeMapping.get(source).getBytes(StandardCharsets.UTF_8));
            result.add(new Entry(entry.mode, entry.name, newId, entry.pathContext));
        }
        return result;
    }

    @Override
    public ObjectId rewriteCommit(final RevCommit commit) {
        //a932ad3be8b5c03c98aed6b3b3c9f6479a6a4261 さいしんこみっと こまったらここ


        //2回目の呼び出しだったらなにもせず帰る
        if (!isFirst) return super.rewriteCommit(commit);

        //デバッグ用に14個くらい実行するやつ
        //if (codeMapping.size() >= 3) return super.rewriteCommit(commit);

        //コミットのIDの取得　SHAとかいうらしい
        String[] commitID = commit.getId().toString().split(" ");

        //実行段階表示
        System.out.println("commit " + commitNumber++ + " / 2204");

        //Git Reset
        String[] gitReset = {"sh", "-c", "git reset --hard " + commitID[1]};
        List<String> gitResetResult = this.execCommand(gitReset);
        System.out.println(gitResetResult);

        //そのコミットで変更されたファイルのうち末尾.javaをとりだし
        //但し、変更すべてだから削除されたファイルも拾ってしまう。
        String[] diff = {"sh", "-c", "git diff --name-only HEAD^ | fgrep \".java\""};
        ArrayList<String> JavaFileList = this.execCommand(diff);

        //javaファイルに変更なかったらなんもしない
        if (JavaFileList.size() == 0) {
            JavaFileList.clear();
            return super.rewriteCommit(commit);
        }

        //Javaのファイルの中身の読み出し
        ArrayList<String> JavaCodeList = new ArrayList<>();
        for (int i = 0; i < JavaFileList.size(); i++) {
            File file = new File(this.config.getSrcPath() + "/" + JavaFileList.get(i));
            if (file.exists()) {
                try {
                    JavaCodeList.add(FileUtils.readFileToString(file, Charset.forName("UTF-8")));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //それが削除されたファイルならなにもしない
                JavaFileList.remove(i);
                i--;
            }
        }

        //2回まわってるからここのことしてるけど、よくわかってない
        for (int i = JavaFileList.size() - 1; i >= 0; i--) {
            if (codeMapping.containsKey(JavaCodeList.get(i))) {
                JavaCodeList.remove(i);
                JavaFileList.remove(i);
            }
        }

        //javaファイルに変更なかったらなんもしない
        if (JavaFileList.size() == 0) {
            JavaFileList.clear();
            JavaCodeList.clear();
            return super.rewriteCommit(commit);
        }

        //Gradle Compile
        String[] gradleCompile = {"sh", "-c", "gradle clean compileJava"};
        List<String> gradleCompileResult = this.execCommand(gradleCompile);
        //コンパイルできなかったらなにもしない
        if (gradleCompileResult.get(2).contains("FAILED")) {
            JavaFileList.clear();
            JavaCodeList.clear();
            return super.rewriteCommit(commit);
        }

        //Mapつくる
        execJavap(JavaFileList, JavaCodeList);
        JavaFileList.clear();
        JavaCodeList.clear();

        return super.rewriteCommit(commit);

    }

    protected List<FinerJavaModule> extractFinerModules(final Entry entry) {
        final String base = entry.pathContext + "/" + entry.name;
        final String source = new String(readBlob(entry.id), StandardCharsets.UTF_8);
        return builder.getFinerJavaModules(base, source);
    }

    //あとでそとにかく
    private ArrayList<String> execCommand(String[] command) {
        String line;
        ArrayList<String> list = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        File dir = new File(String.valueOf(this.config.getSrcPath()));// 実行ディレクトリの指定
        try {
            Process p = runtime.exec(command, null, dir); // 実行ディレクトリ(dir)でCommand(mecab.exe)を実行する
            p.waitFor(); // プロセスの正常終了まで待機させる
            InputStream is = p.getInputStream(); // プロセスの結果を変数に格納する
            BufferedReader br = new BufferedReader(new InputStreamReader(is)); // テキスト読み込みを行えるようにする
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (final IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void execJavap(List<String> JavaFilelist, List<String> JavaCodelist) {
        String line;
        Runtime runtime = Runtime.getRuntime();
        File dir = new File(String.valueOf(this.config.getSrcPath()));// 実行ディレクトリの指定

        try {
            for (int i = 0; i < JavaFilelist.size(); i++) {
                System.out.println(i + 1 + " / " + JavaFilelist.size());
                //ファイル名だけ取り出すのがApache Commonsにあったので
                String name = FilenameUtils.getBaseName(JavaFilelist.get(i));
                //.class のうち　.java が変更されてたものを javap -c
                String[] javap = {"sh", "-c", "find . -name \"*.class\" | grep " + name + ".class | xargs -IXXX javap -c XXX\n"};
                Process p = runtime.exec(javap, null, dir); // 実行ディレクトリ(dir)でCommand(mecab.exe)を実行する
                p.waitFor(); // プロセスの正常終了まで待機させる
                InputStream is = p.getInputStream(); // プロセスの結果を変数に格納する
                BufferedReader br = new BufferedReader(new InputStreamReader(is)); // テキスト読み込みを行えるようにする
                final StringBuffer out = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    out.append(line + "\n");
                }
                if (out.length() != 0) { //javapしたのがnullじゃなかったらマップに追加
                    codeMapping.put(JavaCodelist.get(i), out.toString());
                }
            }
        } catch (final IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


}
