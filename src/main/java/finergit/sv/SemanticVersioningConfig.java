package finergit.sv;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

public class SemanticVersioningConfig implements Cloneable {

  @Option(name = "-a", aliases = "--author", usage = "print author's name of the version")
  private boolean author;

  @Option(name = "-c", aliases = "--commit", usage = "print commit ID of the version")
  private boolean commit;

  @Option(name = "-d", aliases = "--date", usage = "print date of the version")
  private boolean date;

  @Option(name = "-f", aliases = "--follow", usage = "print the history of the specified file")
  private boolean follow;

  @Option(name = "-n", aliases = "--number",
      usage = "print the number of changes of the specified file")
  private boolean number;

  @Option(name = "-p", aliases = "--path", usage = "print path of the specified file")
  private boolean path;

  @Option(name = "-r", aliases = "--reverse", usage = "print the history with the reverse order")
  private boolean reverse;

  @Option(name = "--all",
      usage = "this option means all the \"-a\", \"-c\", \"-d\", \"-f\", \"-n\", and \"-p\" are specified")
  private boolean all;

  @Option(name = "--start-commit", usage = "specify a start commit for counting semantic version")
  private String startCommitId;

  @Option(name = "--end-commit", usage = "specify an end commit for counting semantic version")
  private String endCommitId;

  @Option(name = "--birth-commit", usage = "print the birth commit of the specified file")
  private boolean birthCommit;

  @Option(name = "--birth-date", usage = "print the birth date of the specified file")
  private boolean birthDate;

  @Option(name = "-h", aliases = "--help", usage = "print help for this command")
  private boolean help;

  public final MinimumRenameScore minimumRenameScore;

  @Argument
  private List<String> otherArguments;

  private Path targetFilePath;

  public SemanticVersioningConfig() {
    this.author = false;
    this.commit = false;
    this.date = false;
    this.follow = false;
    this.number = false;
    this.path = false;
    this.reverse = false;
    this.all = false;
    this.help = false;
    this.minimumRenameScore = new MinimumRenameScore();
    this.startCommitId = null;
    this.endCommitId = null;
    this.birthCommit = false;
    this.birthDate = false;
    this.targetFilePath = null;
    this.otherArguments = new ArrayList<>();
  }

  public boolean isAuthor() {
    return this.author || this.all;
  }

  public boolean isCommit() {
    return this.commit || this.all;
  }

  public boolean isDate() {
    return this.date || this.all;
  }

  public boolean isFollow() {
    return this.follow || this.all;
  }

  public boolean isNumber() {
    return this.number || this.all;
  }

  public boolean isPath() {
    return this.path || this.all;
  }

  public boolean isReverse() {
    return this.reverse;
  }

  public boolean isHelp() {
    return this.help;
  }

  public String getStartCommitId() {
    return this.startCommitId;
  }

  public String getEndCommitId() {
    return this.endCommitId;
  }

  public boolean isBirthCommit() {
    return this.birthCommit;
  }

  public boolean isBirthDate() {
    return this.birthDate;
  }

  public List<String> getOtherArguments() {
    return this.otherArguments;
  }

  public void setTargetFilePath(final Path path) {
    this.targetFilePath = path;
  }

  public Path getTargetFilePath() {
    return this.targetFilePath;
  }

  @Option(name = "-l", aliases = "--log-level", metaVar = "<level>",
      usage = "log level (trace, debug, info, warn, error)")
  public void setLogLevel(final String logLevel) {
    final ch.qos.logback.classic.Logger log =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    switch (logLevel.toLowerCase()) {
      case "trace": {
        log.setLevel(Level.TRACE);
        break;
      }
      case "debug": {
        log.setLevel(Level.DEBUG);
        break;
      }
      case "info": {
        log.setLevel(Level.INFO);
        break;
      }
      case "warn": {
        log.setLevel(Level.WARN);
        break;
      }
      case "error": {
        log.setLevel(Level.ERROR);
        break;
      }
      default: {
        System.err.println("inappropriate value for \"-l\" option");
        System.exit(0);
      }
    }
  }

  @Option(name = "-m", aliases = "--minimum-rename-score",
      usage = "specify a minimum score for file rename")
  public void setMinimumRenameScore(final int minimumRenameScore) {
    this.minimumRenameScore.setValue(minimumRenameScore);
  }

  @Override
  public SemanticVersioningConfig clone() {
    final SemanticVersioningConfig clone = new SemanticVersioningConfig();
    clone.author = this.author;
    clone.commit = this.commit;
    clone.date = this.date;
    clone.follow = this.follow;
    clone.number = this.number;
    clone.path = this.path;
    clone.reverse = this.reverse;
    clone.all = this.all;
    clone.help = this.help;
    if (!this.minimumRenameScore.isRepositoryDefault()) {
      clone.minimumRenameScore.setValue(this.minimumRenameScore.getValue());
    }
    clone.startCommitId = this.startCommitId;
    clone.endCommitId = this.endCommitId;
    clone.birthCommit = this.birthCommit;
    clone.birthDate = this.birthDate;
    clone.targetFilePath = this.targetFilePath;
    clone.otherArguments = new ArrayList<>(this.otherArguments);
    return clone;
  }
}
