package com.jobos.android.ui.cv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.jobos.android.R;
import com.jobos.android.ui.util.SystemBarsUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfPreviewActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private LinearLayout pdfPagesContainer;
    private ProgressBar progressBar;
    private TextView loadingText;
    private Uri pdfUri;
    private String pdfUrl;
    private String pdfFileName;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_preview);

        initViews();
        SystemBarsUtil.applyTopInsetToToolbarArea(toolbar);
        setupToolbar();
        loadFromIntent();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        pdfPagesContainer = findViewById(R.id.pdf_pages_container);
        progressBar = findViewById(R.id.progress_bar);
        loadingText = findViewById(R.id.loading_text);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_share) {
                sharePdf();
                return true;
            } else if (id == R.id.action_open_external) {
                openInExternalApp();
                return true;
            }
            return false;
        });
    }

    private void loadFromIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            showError("No data provided");
            return;
        }

        pdfUri = intent.getParcelableExtra("pdfUri");
        pdfUrl = intent.getStringExtra("pdf_url");
        pdfFileName = intent.getStringExtra("pdfFileName");
        String title = intent.getStringExtra("title");
        boolean isTemplate = intent.getBooleanExtra("isTemplate", false);
        String templateCategory = intent.getStringExtra("templateCategory");

        if (title != null) {
            toolbar.setTitle(title);
        } else if (pdfFileName != null) {
            toolbar.setTitle(pdfFileName);
        }

        if (pdfUri != null) {
            renderPdf();
        } else if (isTemplate || (title != null && templateCategory != null)) {
            generateSampleCVPreview(title, templateCategory);
        } else if (pdfUrl != null && !pdfUrl.isEmpty()) {
            if (pdfUrl.toLowerCase().endsWith(".pdf")) {
                downloadAndRenderPdf();
            } else {
                loadImagePreview();
            }
        } else {
            showError("No PDF or image URL provided");
        }
    }

    private void generateSampleCVPreview(String templateName, String category) {
        showLoading(true);
        if (loadingText != null) loadingText.setText("Generating preview...");

        new Thread(() -> {
            try {
                PdfDocument document = new PdfDocument();
                int pageWidth = 595;
                int pageHeight = 842;

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                String primaryColor = "#1a73e8";
                String accentColor = "#34a853";
                if (category != null) {
                    switch (category.toUpperCase()) {
                        case "CREATIVE":
                            primaryColor = "#e91e63";
                            accentColor = "#9c27b0";
                            break;
                        case "MODERN":
                            primaryColor = "#00bcd4";
                            accentColor = "#009688";
                            break;
                        case "MINIMAL":
                            primaryColor = "#424242";
                            accentColor = "#757575";
                            break;
                    }
                }

                Paint headerBgPaint = new Paint();
                headerBgPaint.setColor(Color.parseColor(primaryColor));
                canvas.drawRect(0, 0, pageWidth, 120, headerBgPaint);

                Paint titlePaint = new Paint();
                titlePaint.setColor(Color.WHITE);
                titlePaint.setTextSize(28);
                titlePaint.setFakeBoldText(true);
                titlePaint.setAntiAlias(true);
                canvas.drawText("John Anderson", 50, 60, titlePaint);

                Paint subtitlePaint = new Paint();
                subtitlePaint.setColor(Color.parseColor("#ffffffcc"));
                subtitlePaint.setTextSize(14);
                subtitlePaint.setAntiAlias(true);
                canvas.drawText("Senior Software Engineer", 50, 85, subtitlePaint);
                canvas.drawText("john.anderson@email.com  |  +1 (555) 123-4567  |  San Francisco, CA", 50, 105, subtitlePaint);

                Paint sectionPaint = new Paint();
                sectionPaint.setColor(Color.parseColor(primaryColor));
                sectionPaint.setTextSize(14);
                sectionPaint.setFakeBoldText(true);
                sectionPaint.setAntiAlias(true);

                Paint textPaint = new Paint();
                textPaint.setColor(Color.parseColor("#333333"));
                textPaint.setTextSize(11);
                textPaint.setAntiAlias(true);

                Paint lightTextPaint = new Paint();
                lightTextPaint.setColor(Color.parseColor("#666666"));
                lightTextPaint.setTextSize(10);
                lightTextPaint.setAntiAlias(true);

                int margin = 50;
                int yPos = 150;

                canvas.drawText("PROFESSIONAL SUMMARY", margin, yPos, sectionPaint);
                yPos += 5;
                Paint linePaint = new Paint();
                linePaint.setColor(Color.parseColor(primaryColor));
                linePaint.setStrokeWidth(2);
                canvas.drawLine(margin, yPos, margin + 150, yPos, linePaint);
                yPos += 18;
                canvas.drawText("Results-driven software engineer with 8+ years of experience in full-stack development.", margin, yPos, textPaint);
                yPos += 15;
                canvas.drawText("Proven track record of delivering scalable solutions and leading high-performing teams.", margin, yPos, textPaint);
                yPos += 35;

                canvas.drawText("WORK EXPERIENCE", margin, yPos, sectionPaint);
                yPos += 5;
                canvas.drawLine(margin, yPos, margin + 130, yPos, linePaint);
                yPos += 20;

                Paint boldPaint = new Paint();
                boldPaint.setColor(Color.parseColor("#333333"));
                boldPaint.setTextSize(12);
                boldPaint.setFakeBoldText(true);
                boldPaint.setAntiAlias(true);

                canvas.drawText("Senior Software Engineer", margin, yPos, boldPaint);
                yPos += 15;
                canvas.drawText("TechCorp Inc.  •  San Francisco, CA  •  2020 - Present", margin, yPos, lightTextPaint);
                yPos += 18;
                canvas.drawText("• Led development of microservices architecture serving 10M+ users", margin + 10, yPos, textPaint);
                yPos += 14;
                canvas.drawText("• Mentored team of 5 junior developers and conducted code reviews", margin + 10, yPos, textPaint);
                yPos += 14;
                canvas.drawText("• Reduced deployment time by 60% through CI/CD pipeline optimization", margin + 10, yPos, textPaint);
                yPos += 28;

                canvas.drawText("Software Engineer", margin, yPos, boldPaint);
                yPos += 15;
                canvas.drawText("StartupXYZ  •  New York, NY  •  2017 - 2020", margin, yPos, lightTextPaint);
                yPos += 18;
                canvas.drawText("• Developed RESTful APIs handling 1M+ daily requests", margin + 10, yPos, textPaint);
                yPos += 14;
                canvas.drawText("• Implemented real-time notification system using WebSockets", margin + 10, yPos, textPaint);
                yPos += 35;

                canvas.drawText("SKILLS", margin, yPos, sectionPaint);
                yPos += 5;
                canvas.drawLine(margin, yPos, margin + 50, yPos, linePaint);
                yPos += 20;

                Paint skillBgPaint = new Paint();
                skillBgPaint.setColor(Color.parseColor("#e8f0fe"));
                Paint skillTextPaint = new Paint();
                skillTextPaint.setColor(Color.parseColor(primaryColor));
                skillTextPaint.setTextSize(10);
                skillTextPaint.setAntiAlias(true);

                String[] skills = {"Java", "Python", "JavaScript", "React", "Node.js", "AWS", "Docker", "Kubernetes"};
                int xPos = margin;
                for (String skill : skills) {
                    float textWidth = skillTextPaint.measureText(skill);
                    canvas.drawRoundRect(xPos, yPos - 12, xPos + textWidth + 16, yPos + 6, 10, 10, skillBgPaint);
                    canvas.drawText(skill, xPos + 8, yPos, skillTextPaint);
                    xPos += textWidth + 24;
                    if (xPos > pageWidth - margin - 60) {
                        xPos = margin;
                        yPos += 25;
                    }
                }
                yPos += 35;

                canvas.drawText("EDUCATION", margin, yPos, sectionPaint);
                yPos += 5;
                canvas.drawLine(margin, yPos, margin + 80, yPos, linePaint);
                yPos += 20;
                canvas.drawText("Master of Science in Computer Science", margin, yPos, boldPaint);
                yPos += 15;
                canvas.drawText("Stanford University  •  2015 - 2017", margin, yPos, lightTextPaint);
                yPos += 25;
                canvas.drawText("Bachelor of Science in Computer Science", margin, yPos, boldPaint);
                yPos += 15;
                canvas.drawText("MIT  •  2011 - 2015", margin, yPos, lightTextPaint);

                Paint footerPaint = new Paint();
                footerPaint.setColor(Color.parseColor("#999999"));
                footerPaint.setTextSize(9);
                footerPaint.setAntiAlias(true);
                String footerText = "Sample CV generated with " + (templateName != null ? templateName : "JobOS") + " template";
                float footerWidth = footerPaint.measureText(footerText);
                canvas.drawText(footerText, (pageWidth - footerWidth) / 2, pageHeight - 30, footerPaint);

                document.finishPage(page);

                File cacheDir = getCacheDir();
                File pdfFile = new File(cacheDir, "sample_cv_preview.pdf");
                FileOutputStream fos = new FileOutputStream(pdfFile);
                document.writeTo(fos);
                fos.close();
                document.close();

                pdfUri = Uri.fromFile(pdfFile);

                runOnUiThread(() -> renderPdfFromFile(pdfFile));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error generating preview: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadImagePreview() {
        showLoading(true);
        if (loadingText != null) loadingText.setText("Loading preview...");

        runOnUiThread(() -> {
            pdfPagesContainer.removeAllViews();

            MaterialCardView cardView = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            cardView.setLayoutParams(cardParams);
            cardView.setCardElevation(4);
            cardView.setRadius(12);

            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setMinimumHeight(800);

            Glide.with(this)
                    .load(pdfUrl)
                    .placeholder(R.drawable.ic_document)
                    .error(R.drawable.ic_document)
                    .into(imageView);

            cardView.addView(imageView);
            pdfPagesContainer.addView(cardView);

            showLoading(false);
        });
    }

    private void downloadAndRenderPdf() {
        showLoading(true);
        if (loadingText != null) loadingText.setText("Downloading PDF...");

        new Thread(() -> {
            try {
                URL url = new URL(pdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                File cacheDir = getCacheDir();
                File pdfFile = new File(cacheDir, "temp_preview.pdf");

                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(pdfFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();
                connection.disconnect();

                pdfUri = Uri.fromFile(pdfFile);

                runOnUiThread(() -> {
                    if (loadingText != null) loadingText.setText("Rendering PDF...");
                    renderPdfFromFile(pdfFile);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error downloading PDF: " + e.getMessage());
                });
            }
        }).start();
    }

    private void renderPdfFromFile(File pdfFile) {
        new Thread(() -> {
            try {
                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(fileDescriptor);
                int pageCount = pdfRenderer.getPageCount();

                runOnUiThread(() -> {
                    pdfPagesContainer.removeAllViews();
                    for (int i = 0; i < pageCount; i++) {
                        renderPage(i);
                    }
                    showLoading(false);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error rendering PDF: " + e.getMessage());
                });
            }
        }).start();
    }

    private void renderPdf() {
        showLoading(true);
        if (loadingText != null) loadingText.setText("Rendering PDF...");

        new Thread(() -> {
            try {
                fileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
                if (fileDescriptor == null) {
                    runOnUiThread(() -> showError("Unable to open PDF file"));
                    return;
                }

                pdfRenderer = new PdfRenderer(fileDescriptor);
                int pageCount = pdfRenderer.getPageCount();

                runOnUiThread(() -> {
                    pdfPagesContainer.removeAllViews();
                    for (int i = 0; i < pageCount; i++) {
                        renderPage(i);
                    }
                    showLoading(false);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error rendering PDF: " + e.getMessage());
                });
            }
        }).start();
    }

    private void renderPage(int pageIndex) {
        try {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);

            int width = getResources().getDisplayMetrics().widthPixels - 64;
            float scale = (float) width / page.getWidth();
            int height = (int) (page.getHeight() * scale);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(0xFFFFFFFF);

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            MaterialCardView cardView = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            cardView.setLayoutParams(cardParams);
            cardView.setCardElevation(4);
            cardView.setRadius(12);

            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            imageView.setAdjustViewBounds(true);
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            cardView.addView(imageView);
            pdfPagesContainer.addView(cardView);

        } catch (Exception e) {
            Toast.makeText(this, "Error rendering page " + (pageIndex + 1), Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf() {
        if (pdfUri == null) return;

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void openInExternalApp() {
        if (pdfUri == null) return;

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer app installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (loadingText != null) loadingText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (Exception ignored) {}
    }
}
