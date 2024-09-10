package mindustrymod.jslogic;

public class JsConsole {
  JsExecutor executor;

  private StringBuilder logContent;

  public JsConsole(JsExecutor executor) {
      this.executor = executor;
      this.logContent = new StringBuilder();
  }

  public void clear() {
      logContent.setLength(0);
      if (executor.consoleListener != null)
          executor.consoleListener.get(getLogContent());
  }

  public void log(String string) {
      appendMessage("LOG: ", string);
  }

  public void warn(String string) {
      appendMessage("WARN: ", string);
  }

  public void error(String string) {
      appendMessage("ERROR: ", string);
  }

  private void appendMessage(String prefix, String string) {
      logContent.append(prefix);
      logContent.append(string).append(" ");
      logContent.append("\n");
      if(logContent.length() > 1000){
            logContent = new StringBuilder(logContent.substring(logContent.length() - 1000));
      }
      if (executor.consoleListener != null)
          executor.consoleListener.get(getLogContent());
  }

  public String getLogContent() {
    if(logContent.length() > 1000){
        return logContent.substring(logContent.length() - 1000).toString();
    }else{
        return logContent.toString();
    }
  }
}    