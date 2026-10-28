Voice-match assets (Flora ke apne, Apache-2.0, HF tflite-hub se):
- conformer_tisid_medium.tflite — speaker encoder (d-vector)
- vad_long_model.tflite — speech activity detector
- vad_long_mean_stddev.csv — VAD normalisation (mean/stddev, 2x128)
- mel_matrix_f32.bin — TensorFlow linear_to_mel_weight_matrix
  (513x128 float32 LE; TF se generate kiya gaya, HTK formula NAHI)
Maya/creator ka koi model yahan nahi hai.
