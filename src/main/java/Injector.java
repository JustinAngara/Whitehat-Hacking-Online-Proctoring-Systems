import java.io.IOException;

public class Injector {
    public static void main(String[] args) throws IOException {
        String dllPath = "C:\\Users\\justi\\IdeaProjects\\Honorlock\\src\\main\\c\\payload.dll";
        String processName = "Spotify.exe"; // or Spotify.exe, etc.

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Users\\justi\\IdeaProjects\\Honorlock\\src\\main\\c\\injector.exe",
                dllPath, processName
        );
        pb.inheritIO();
        pb.start();
    }
}
