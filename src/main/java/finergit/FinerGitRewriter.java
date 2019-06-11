package finergit;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class FinerGitRewriter extends ConcurrentRepositoryRewriter {

  private static final Logger log = LoggerFactory.getLogger(FinerGitRewriter.class);
  private final FinerGitConfig config;
  private final FinerJavaFileBuilder builder;
  public static Map<String ,Map<String ,String >> CodeJavaFileNameJavapCodeMap = new HashMap<>();  //staticよくない
  private Map<String,List<String>> commitIDJavaFileMapping = new HashMap<>();
  private List<String> javaFileNameList = new ArrayList<String>();
  private boolean isFirst;

  List<String> JavaCodelist = new ArrayList<String>();
  List<String> JavaFilelist = new ArrayList<String>();

  public FinerGitRewriter(final FinerGitConfig config) {
    this.config = config;
    this.builder = new FinerJavaFileBuilder(config);
    setConcurrent(config.isParallel());
    setPathSensitive(true);
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
      //result.add(new Entry(entry.mode, name, newId, entry.pathContext));
    }*/

    /* メソッド1行にしたファイルの作成 */
    /*
    final String base = entry.pathContext + "/" + entry.name;
    final String source = new String(readBlob(entry.id), StandardCharsets.UTF_8);
    final String txt=builder.getFinerJavaString(base,source);
    final ObjectId newId = writeBlob(txt.getBytes(StandardCharsets.UTF_8));
    result.add(new Entry(entry.mode, entry.name, newId, entry.pathContext));
    */

    /* javapした.classファイルをエントリに追加
    * entry.mode 100644 ってなんだろ
    * entry.pathContext /src/main/java/jp/kusumotolab/kgenprog/project/build こんなん
    *  */
    //System.out.println(CodeJavaFileNameJavapCodeMap);

    final String source = new String(readBlob(entry.id), StandardCharsets.UTF_8);
    System.out.println(source);
    if (CodeJavaFileNameJavapCodeMap.containsKey(source)) {
      //ぁぁぁ　ここできない
      System.out.println("OK");
      System.out.println(CodeJavaFileNameJavapCodeMap.containsKey(source));
    }


    return result;
  }

  @Override
  public ObjectId rewriteCommit(final RevCommit commit) {

    String[] commitID = commit.getId().toString().split(" ");
    if(commitID[1].equals("9cafd2e0f1630300700bf672ac461596147fb92b")) {
      System.out.println(" ** rewrite commit ** ");

      String[] gitReset = {"sh","-c","git reset --hard 9cafd2e0f1630300700bf672ac461596147fb92b"};
      List<String> gitResetResult = this.execCommand(gitReset);
      String[] gradleCompile = {"sh","-c","gradle clean compileJava"};
      List<String> gradleCompileResult = this.execCommand(gradleCompile);


      //String[] diff = {"sh","git diff --stat --name-only 9cafd2e0 7f124e07 | grep .java\n"};
      String[] kari = {"sh","-c","find . -name \"*.java\" | grep -v \"/example/\" | grep -v \"/test/\""};
      //String[] diff = {"git diff --stat --name-only " + commitID[i] + " " + commitID[i-1] + "| grep .java"};
      List<String> JavaFileList = this.execCommand(kari);
      System.out.println(JavaFileList);


      //Javaのファイルの中身の読み出し
      for(int i = 0; i < JavaFileList.size(); i++){
        try {
          JavaCodelist.add(readAll(JavaFileList.get(i).replaceFirst("\\.","/Users/a-maejim/Documents/kGenProg")));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      //<Javaコード < javaファイル名　javapコード>　Map
      Map<String,String> javapmapping = execJavap(JavaFileList,JavaCodelist);
      System.out.println(CodeJavaFileNameJavapCodeMap);


    }



    return super.rewriteCommit(commit);

  }

    protected List<FinerJavaModule> extractFinerModules(final Entry entry) {
    final String base = entry.pathContext + "/" + entry.name;
    final String source = new String(readBlob(entry.id), StandardCharsets.UTF_8);
    return builder.getFinerJavaModules(base, source);
  }

  //あとでそとにかく
  private List<String> execCommand(String[] Command){
    String line;
    List<String> list = new ArrayList<String>();
    Runtime runtime = Runtime.getRuntime();
    File dir = new File("/Users/a-maejim/Documents/kGenProg");// 実行ディレクトリの指定
    try {
      Process p = runtime.exec(Command, null, dir); // 実行ディレクトリ(dir)でCommand(mecab.exe)を実行する
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

  private Map<String,String> execJavap(List<String> list,List<String> JavaCodelist){
    String line;
    Map<String,String> map = new HashMap<>();
    Runtime runtime = Runtime.getRuntime();
    File dir = new File("/Users/a-maejim/Documents/kGenProg");// 実行ディレクトリの指定

    try {
      for(int i = 0; i < 5; i++){
        System.out.println(i + " / " + list.size());

        String[] fileName = list.get(i).split("/", 0);
        String name = fileName[fileName.length-1].split("\\.")[0];
        //System.out.println(name);
        String[] javap = {"sh","-c","find . -name \"*.class\" | grep " + name +".class | xargs -IXXX javap -c XXX\n"};
        Process p = runtime.exec(javap, null, dir); // 実行ディレクトリ(dir)でCommand(mecab.exe)を実行する
        p.waitFor(); // プロセスの正常終了まで待機させる
        InputStream is = p.getInputStream(); // プロセスの結果を変数に格納する
        BufferedReader br = new BufferedReader(new InputStreamReader(is)); // テキスト読み込みを行えるようにする
        final StringBuffer out = new StringBuffer();
        while ((line = br.readLine()) != null) {
          out.append(line + "\n");
          //System.out.println(line);
        }
        if(out.length()!=0) { //javapしたのがnullじゃなかったらマップに追加
          CodeJavaFileNameJavapCodeMap.put(JavaCodelist.get(i), new HashMap<>());
          CodeJavaFileNameJavapCodeMap.get(JavaCodelist.get(i)).put(list.get(i), out.toString());
          //System.out.println(out.toString());
        }
      }
    } catch (final IOException | InterruptedException e) {
      e.printStackTrace();
    }
    return map;
  }

  public static String readAll(final String path) throws IOException {
    return Files.lines(Paths.get(path), Charset.forName("UTF-8"))
            .collect(Collectors.joining(System.getProperty("line.separator")));
  }


}
