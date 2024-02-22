package com.example.myappvision;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

import android.Manifest;

public class MainActivity extends AppCompatActivity
        implements OnSuccessListener<Text>, OnFailureListener {
    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;

    Bitmap mSelectedImage;
    ImageView mImageView;
    TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Permiso de ejecucion
        if (checkSelfPermission(Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);

        //Referencia TextView y ImageView
        mImageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);
    }


    //Carga los archivos a la GUI
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void abrirGaleria (View view){
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }
    public void abrirCamera (View view){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }
    public void OCRfx(View v) {
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }
    public void Rostrosfx(View v) {
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();
        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.size() == 0) {
                            txtResults.setText("No Hay rostros");
                        }else{
                            txtResults.setText("Hay " + faces.size() + " rostro(s)");
                        }

                        //Colorea los rostros
                        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
                        Bitmap bitmap = drawable.getBitmap().copy(Bitmap.Config.ARGB_8888,true);
                        Canvas canvas = new Canvas(bitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(5);
                        paint.setStyle(Paint.Style.STROKE);
                        for (Face rostro:faces) {
                            canvas.drawRect(rostro.getBoundingBox(), paint);
                        }
                        mImageView.setImageBitmap(bitmap);
                    }
                })
                .addOnFailureListener(this);
    }

public void CodeQR(View view){
    // [START set_detector_options]
    // Configuracon de lectura QR
    BarcodeScannerOptions options =
            new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                            Barcode.FORMAT_QR_CODE,
                            Barcode.FORMAT_AZTEC)
                    .build();

    InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

    // START de QR
    BarcodeScanner scanner = BarcodeScanning.getClient();
    // [START run_detector]
    Task<List<Barcode>> result = scanner.process(image)
            .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                @Override
                public void onSuccess(List<Barcode> barcodes) {
                    StringBuilder resultBuilderQR = new StringBuilder();
                    // Obtiene el valor del código de QR
                    for (Barcode barcode : barcodes) {
                        String barcodeValue = barcode.getRawValue();
                        resultBuilderQR.append(barcodeValue).append("\n");
                    }
                    txtResults.setText(resultBuilderQR.toString());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    txtResults.setText("Error en la lectura de Codigo QR...");
                }
            });
    // [END run_detector]
}

public void CodeBarra(View view){
    BarcodeScannerOptions options =
            new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODABAR,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_CODABAR,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_PDF417,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_ALL_FORMATS)
                    .build();

    InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

    BarcodeScanner scanner = BarcodeScanning.getClient();

    Task<List<Barcode>> result = scanner.process(image)
            .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                @Override
                public void onSuccess(List<Barcode> barcodes) {
                    StringBuilder resultBuilderBarra = new StringBuilder();
                    for (Barcode barcode : barcodes) {
                        String barcodeValue = barcode.getRawValue();
                        resultBuilderBarra.append(barcodeValue).append("\n");
                    }
                    txtResults.setText(resultBuilderBarra.toString());
                    Log.d("","Info: "+resultBuilderBarra.toString());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    txtResults.setText("Error en la lectura del Codigo de Barra...");
                }
            });
}

    @Override
    public void onFailure(@NonNull Exception e) {
        txtResults.setText("Error al procesar imagen");
    }

    @Override
    public void onSuccess(Text text) {

        List<Text.TextBlock> blocks = text.getTextBlocks();
        String resultados="";
        if (blocks.size() == 0) {
            resultados = "No hay Texto";
        }else{
            for (int i = 0; i < blocks.size(); i++) {
                List<Text.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    List<Text.Element> elements = lines.get(j).getElements();
                    for (int k = 0; k < elements.size(); k++) {
                        resultados = resultados + elements.get(k).getText() + " ";
                    }
                }
            }
            resultados=resultados + "\n";
        }
        txtResults.setText(resultados);
    }

}