
import java.io.*;

//Message class to store user messages
public class Message implements Serializable {

    private final MessageType type;
    private final String text;
    private final String from;
    private final String when;
    private final byte[] fileContent;
    private final File file;

    Message(MessageType type, String text, byte[] fileContent, File file, String from, String when) {
        this.type = type;
        this.text = text;        
        this.from = from;
        this.when = when;
        this.fileContent = fileContent;
        this.file = file;
    }

    public String getFrom() {
        return from;
    }

    public String getWhen() {
        return when;
    }

    MessageType getType() {
        return type;
    }

    byte[] getFileContent() {
        return fileContent;
    }

    File getFile() {
        return file;
    }

    String getText() {
        return text;
    }
}
