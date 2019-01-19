import tensorflow as tf

# In Tensorflow 1.12, lite still under contrib package
converter = tf.contrib.lite.TFLiteConverter.from_keras_model_file("fashion_mnist.h5")
tflite_model = converter.convert()
open("fashion_mnist.tflite", "wb").write(tflite_model)
