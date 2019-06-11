package finergit;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

/**
 * Gitリポジトリから細粒度リポジトリの生成処理を行うクラス
 *
 */
public class FinerRepoBuilder {

  private static final Logger log = LoggerFactory.getLogger(FinerRepoBuilder.class);

  private final FinerGitConfig config;

  public FinerRepoBuilder(final FinerGitConfig config) {
    log.trace("enter FinerRepoBuilder(FinerGitConfig)");
    this.config = config;
  }

  public GitRepo exec(boolean isFirstTime) {
    log.trace("enter exec()");
    GitRepo repo = null;
    try {
      // duplicate repository
      copyDirectory(this.config.getSrcPath(), this.config.getDesPath());
      repo = new GitRepo(this.config.getDesPath());
      repo.initialize();
      repo.setIgnoreCase(false);

      final FinerGitRewriter rewriter = new FinerGitRewriter(config);
      rewriter.initialize(repo.getRepository());
      rewriter.rewrite();

      // clean up working copy
      final boolean resetSucceeded = repo.resetHard();
      log.debug("git reset --hard: {}", resetSucceeded ? "succeeded" : "failed");
      final boolean cleanSucceeded = repo.clean();
      log.debug("git clean -fd: {}", cleanSucceeded ? "succeeded" : "failed");

      if(isFirstTime) {
        deleteDirectory("/Users/a-maejim/Documents/kGenProg-fg-sample");
      }

    } catch (final Exception e) {
      e.printStackTrace();
    }
    log.trace("exit exec()");
    return repo;
  }

  /**
   * Copy a directory recursively.
   */
  protected void copyDirectory(final Path source, final Path target) throws IOException {
    log.debug("Copy directory: {} to {}", source, target);
    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
          throws IOException {
        Files.createDirectories(target.resolve(source.relativize(dir)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
          throws IOException {
        Files.copy(file, target.resolve(source.relativize(file)), LinkOption.NOFOLLOW_LINKS);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static void deleteDirectory(final String dirPath) throws Exception {
    File file = new File(dirPath);
    recursiveDeleteFile(file);
  }
  private static void recursiveDeleteFile(final File file) throws Exception {
    // 存在しない場合は処理終了
    if (!file.exists()) {
      return;
    }
    // 対象がディレクトリの場合は再帰処理
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        recursiveDeleteFile(child);
      }
    }
    // 対象がファイルもしくは配下が空のディレクトリの場合は削除する
    file.delete();
  }

}
