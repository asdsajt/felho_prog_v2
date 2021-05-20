package functions;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TranslateText implements BackgroundFunction<Message> {
  private static final Logger logger = Logger.getLogger(TranslateText.class.getName());

  // TODO<developer> set these environment variables
  private static final String PROJECT_ID = getenv("GCP_PROJECT");
  private static final String T2S_TOPIC_NAME = getenv("T2S_TOPIC");
  private static final String LOCATION_NAME = LocationName.of(PROJECT_ID, "global").toString();

  private Publisher publisher;

  public TranslateText() throws IOException {
    publisher = Publisher.newBuilder(
        ProjectTopicName.of(PROJECT_ID, T2S_TOPIC_NAME)).build();
  }

  @Override
  public void accept(Message message, Context context) {
    ApiMessage apiMessage = ApiMessage.fromPubsubData(
        message.getData().getBytes(StandardCharsets.UTF_8));

    String targetLang = "hu";

    TranslateTextRequest request =
        TranslateTextRequest.newBuilder()
            .setParent(LOCATION_NAME)
            .setMimeType("text/plain")
            .setTargetLanguageCode(targetLang)
            .addContents(apiMessage.getText())
            .build();

    TranslateTextResponse response;
    try (TranslationServiceClient client = TranslationServiceClient.create()) {
      response = client.translateText(request);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error translating text: " + e.getMessage(), e);
      return;
    }
    if (response.getTranslationsCount() == 0) {
      return;
    }

    String translatedText = response.getTranslations(0).getTranslatedText();

    // Send translated text to (subsequent) Pub/Sub topic
    String filename = apiMessage.getFilename();
    ApiMessage translateMessage = new ApiMessage(
        translatedText, filename, targetLang);
    try {
      ByteString byteStr = ByteString.copyFrom(translateMessage.toPubsubData());
      PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();

      publisher.publish(pubsubApiMessage).get();
      logger.info("Text translated to " + targetLang);
    } catch (InterruptedException | ExecutionException e) {
      // Log error (since these exception types cannot be thrown by a function)
      logger.log(Level.SEVERE, "Error publishing translation save request: " + e.getMessage(), e);
    }
  }

  private static String getenv(String name) {
    String value = System.getenv(name);
    if (value == null) {
      logger.warning("Environment variable " + name + " was not set");
      value = "MISSING";
    }
    return value;
  }
}
