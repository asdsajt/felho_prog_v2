package functions;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.texttospeech.v1.*;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextToSpeech implements BackgroundFunction<Message> {

  private static final String RESULT_BUCKET = System.getenv("RESULT_BUCKET");

  private static final Storage STORAGE = StorageOptions.getDefaultInstance().getService();
  private static final Logger logger = Logger.getLogger(TextToSpeech.class.getName());

  @Override
  public void accept(Message message, Context context) {
    ApiMessage apiMessage = ApiMessage.fromPubsubData(
            message.getData().getBytes(StandardCharsets.UTF_8));

    try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
      // Set the text input to be synthesized
      SynthesisInput input = SynthesisInput.newBuilder().setText(apiMessage.getText()).build();

      // Build the voice request
      VoiceSelectionParams voice =
              VoiceSelectionParams.newBuilder()
                      .setLanguageCode("hu-HU")
                      .setSsmlGender(SsmlVoiceGender.FEMALE)
                      .build();

      // Select the type of audio file you want returned
      AudioConfig audioConfig =
              AudioConfig.newBuilder()
                      .setAudioEncoding(AudioEncoding.MP3) // MP3 audio.
                      .build();

      // Perform the text-to-speech request
      SynthesizeSpeechResponse responseAudio =
              textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

      // Get the audio contents from the response
      ByteString audioContents = responseAudio.getAudioContent();

      String newFileName = String.format(
              "%s_to_%s.mp3", apiMessage.getFilename(), "hu");

      logger.info(String.format("Saving result to %s in bucket %s", newFileName, RESULT_BUCKET));
      BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(RESULT_BUCKET, newFileName)).build();
      STORAGE.create(blobInfo, audioContents.toByteArray());
      logger.info("File saved");

    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error publishing translation save request: " + e.getMessage(), e);
    }
  }
}
