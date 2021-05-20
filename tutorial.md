# initial configs
gsutil mb gs://kswov1_image_bucket_v2
gsutil mb gs://kswov1_result_bucket_v2
gcloud pubsub topics create kswov1_translate_topic
gcloud pubsub topics create kswov1_text2speech_topic

# clone the repo
git clone https://github.com/asdsajt/felho_beadando.git

# go to process-image folder
cd felho_beadando/process-image/

# deploying the function
gcloud functions deploy kswov1-ocr \
--entry-point functions.OcrProcessImage \
--runtime java11 \
--memory 512MB \
--trigger-bucket kswov1_image_bucket_v2 \
--set-env-vars "^:^GCP_PROJECT=kswov1-beadando-313911:TRANSLATE_TOPIC=kswov1_translate_topic:TO_LANG=hu"

# go to translate folder
cd ../translate/

# deploying the function
gcloud functions deploy kswov1-translate \
--entry-point functions.TranslateText \
--runtime java11 \
--memory 512MB \
--trigger-topic kswov1_translate_topic \
--set-env-vars "GCP_PROJECT=kswov1-beadando-313911,T2S_TOPIC=kswov1_text2speech_topic"

# go to translate folder
cd ../text-to-speech/

# deploying the function
gcloud functions deploy kswov1-t2s \
--entry-point functions.TextToSpeech \
--runtime java11 \
--memory 512MB \
--trigger-topic kswov1_text2speech_topic \
--set-env-vars "GCP_PROJECT=kswov1-beadando-313911,RESULT_BUCKET=kswov1_result_bucket_v2"

