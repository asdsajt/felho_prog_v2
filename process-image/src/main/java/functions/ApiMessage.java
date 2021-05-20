package functions;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class ApiMessage {
  private static final Gson gson = new Gson();

  private String text;
  private String filename;
  private String lang;

  public ApiMessage(String text, String filename, String lang) {
    if (text == null) {
      throw new IllegalArgumentException("Missing text parameter");
    }
    if (filename == null) {
      throw new IllegalArgumentException("Missing filename parameter");
    }
    if (lang == null) {
      throw new IllegalArgumentException("Missing lang parameter");
    }

    this.text = text;
    this.filename = filename;
    this.lang = lang;
  }
  public byte[] toPubsubData() {
    return gson.toJson(this).getBytes(StandardCharsets.UTF_8);
  }
}
