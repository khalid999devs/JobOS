package com.jobos.android.ui.cv;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.cv.CVDTO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class CVPreviewFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextView fullName;
    private TextView email;
    private TextView phone;
    private TextView address;
    private MaterialCardView summaryCard;
    private TextView summary;
    private MaterialCardView skillsCard;
    private ChipGroup skillsChipGroup;
    private MaterialCardView experienceCard;
    private LinearLayout experienceContainer;
    private MaterialCardView educationCard;
    private LinearLayout educationContainer;
    private MaterialCardView linksCard;
    private TextView linkedinLink;
    private TextView portfolioLink;
    private ProgressBar progressBar;

    private String cvId = null;
    private ApiService apiService;
    private CVDTO currentCV;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cv_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        hideBottomNav();

        if (getArguments() != null) {
            cvId = getArguments().getString("cvId");
        }

        initViews(view);
        setupClickListeners();
        loadCVDetails();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        fullName = view.findViewById(R.id.full_name);
        email = view.findViewById(R.id.email);
        phone = view.findViewById(R.id.phone);
        address = view.findViewById(R.id.address);
        summaryCard = view.findViewById(R.id.summary_card);
        summary = view.findViewById(R.id.summary);
        skillsCard = view.findViewById(R.id.skills_card);
        skillsChipGroup = view.findViewById(R.id.skills_chip_group);
        experienceCard = view.findViewById(R.id.experience_card);
        experienceContainer = view.findViewById(R.id.experience_container);
        educationCard = view.findViewById(R.id.education_card);
        educationContainer = view.findViewById(R.id.education_container);
        linksCard = view.findViewById(R.id.links_card);
        linkedinLink = view.findViewById(R.id.linkedin_link);
        portfolioLink = view.findViewById(R.id.portfolio_link);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                if (cvId != null) {
                    Bundle args = new Bundle();
                    args.putString("cvId", cvId);
                    navController.navigate(R.id.cvEditorFragment, args);
                }
                return true;
            } else if (id == R.id.action_share) {
                shareCV();
                return true;
            } else if (id == R.id.action_download) {
                downloadAsPdf();
                return true;
            }
            return false;
        });

        linkedinLink.setOnClickListener(v -> {
            String url = linkedinLink.getText().toString();
            if (!url.isEmpty()) openUrl(url);
        });

        portfolioLink.setOnClickListener(v -> {
            String url = portfolioLink.getText().toString();
            if (!url.isEmpty()) openUrl(url);
        });
    }

    private void loadCVDetails() {
        if (cvId == null) {
            showToast("Invalid CV");
            navController.popBackStack();
            return;
        }

        showLoading(true);
        apiService.getCVDetails(sessionManager.getAccessToken(), cvId,
            new ApiCallback<CVDTO>() {
                @Override
                public void onSuccess(CVDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        currentCV = result;
                        displayCV();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading CV: " + error);
                    });
                }
            });
    }

    private void displayCV() {
        if (currentCV == null) return;

        toolbar.setTitle(currentCV.getTitle());
        
        String fullNameText = currentCV.getFullName();
        if (fullNameText != null && !fullNameText.isEmpty()) {
            fullName.setText(fullNameText);
        }
        
        String emailText = currentCV.getEmail();
        if (emailText != null && !emailText.isEmpty()) {
            email.setText(emailText);
        }

        String phoneText = currentCV.getPhone();
        if (phoneText != null && !phoneText.isEmpty()) {
            phone.setText(phoneText);
            phone.setVisibility(View.VISIBLE);
        } else {
            phone.setVisibility(View.GONE);
        }

        String addressText = currentCV.getAddress();
        if (addressText != null && !addressText.isEmpty()) {
            address.setText(addressText);
            address.setVisibility(View.VISIBLE);
        } else {
            address.setVisibility(View.GONE);
        }

        String summaryText = currentCV.getSummary();
        if (summaryText != null && !summaryText.isEmpty() && !summaryText.equals("null")) {
            summary.setText(summaryText);
            summaryCard.setVisibility(View.VISIBLE);
        } else {
            summaryCard.setVisibility(View.GONE);
        }

        List<String> skills = currentCV.getSkills();
        if (skills != null && !skills.isEmpty()) {
            skillsChipGroup.removeAllViews();
            for (String skill : skills) {
                if (skill != null && !skill.trim().isEmpty() && !skill.equals("null")) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(skill.trim());
                    chip.setClickable(false);
                    skillsChipGroup.addView(chip);
                }
            }
            if (skillsChipGroup.getChildCount() > 0) {
                skillsCard.setVisibility(View.VISIBLE);
            } else {
                skillsCard.setVisibility(View.GONE);
            }
        } else {
            skillsCard.setVisibility(View.GONE);
        }

        List<String> experience = currentCV.getExperience();
        if (experience != null && !experience.isEmpty()) {
            experienceContainer.removeAllViews();
            for (String exp : experience) {
                if (exp != null && !exp.trim().isEmpty() && !exp.equals("null")) {
                    TextView textView = createSectionItem(exp.trim());
                    experienceContainer.addView(textView);
                }
            }
            if (experienceContainer.getChildCount() > 0) {
                experienceCard.setVisibility(View.VISIBLE);
            } else {
                experienceCard.setVisibility(View.GONE);
            }
        } else {
            experienceCard.setVisibility(View.GONE);
        }

        List<String> education = currentCV.getEducation();
        if (education != null && !education.isEmpty()) {
            educationContainer.removeAllViews();
            for (String edu : education) {
                if (edu != null && !edu.trim().isEmpty() && !edu.equals("null")) {
                    TextView textView = createSectionItem(edu.trim());
                    educationContainer.addView(textView);
                }
            }
            if (educationContainer.getChildCount() > 0) {
                educationCard.setVisibility(View.VISIBLE);
            } else {
                educationCard.setVisibility(View.GONE);
            }
        } else {
            educationCard.setVisibility(View.GONE);
        }

        String linkedin = currentCV.getLinkedinUrl();
        String portfolio = currentCV.getPortfolioUrl();
        boolean hasLinks = false;

        if (linkedin != null && !linkedin.isEmpty() && !linkedin.equals("null")) {
            linkedinLink.setText(linkedin);
            linkedinLink.setVisibility(View.VISIBLE);
            hasLinks = true;
        } else {
            linkedinLink.setVisibility(View.GONE);
        }

        if (portfolio != null && !portfolio.isEmpty() && !portfolio.equals("null")) {
            portfolioLink.setText(portfolio);
            portfolioLink.setVisibility(View.VISIBLE);
            hasLinks = true;
        } else {
            portfolioLink.setVisibility(View.GONE);
        }

        if (hasLinks) {
            linksCard.setVisibility(View.VISIBLE);
        } else {
            linksCard.setVisibility(View.GONE);
        }
    }

    private TextView createSectionItem(String text) {
        TextView textView = new TextView(requireContext());
        textView.setText("• " + text);
        textView.setTextSize(14);
        textView.setTextColor(getResources().getColor(R.color.on_surface, null));
        textView.setLineSpacing(4, 1.0f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        textView.setLayoutParams(params);
        return textView;
    }

    private void shareCV() {
        if (currentCV == null) return;

        StringBuilder shareText = new StringBuilder();
        shareText.append(currentCV.getFullName()).append("\n");
        shareText.append(currentCV.getEmail()).append("\n\n");

        if (currentCV.getSummary() != null) {
            shareText.append("Summary:\n").append(currentCV.getSummary()).append("\n\n");
        }

        List<String> skills = currentCV.getSkills();
        if (skills != null && !skills.isEmpty()) {
            shareText.append("Skills: ").append(String.join(", ", skills)).append("\n\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentCV.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share CV"));
    }

    private void openUrl(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            showToast("Unable to open link");
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void downloadAsPdf() {
        if (currentCV == null) {
            showToast("No CV data to download");
            return;
        }

        try {
            PdfDocument document = new PdfDocument();
            
            int pageWidth = 595;
            int pageHeight = 842;
            
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.parseColor("#1a73e8"));
            titlePaint.setTextSize(24);
            titlePaint.setFakeBoldText(true);
            
            Paint headerPaint = new Paint();
            headerPaint.setColor(Color.parseColor("#333333"));
            headerPaint.setTextSize(16);
            headerPaint.setFakeBoldText(true);
            
            Paint textPaint = new Paint();
            textPaint.setColor(Color.parseColor("#666666"));
            textPaint.setTextSize(12);
            
            Paint contactPaint = new Paint();
            contactPaint.setColor(Color.parseColor("#888888"));
            contactPaint.setTextSize(11);
            
            int margin = 50;
            int yPos = margin;
            int lineSpacing = 20;
            int sectionSpacing = 30;
            
            String fullNameText = currentCV.getFullName();
            if (fullNameText != null) {
                canvas.drawText(fullNameText, margin, yPos, titlePaint);
                yPos += lineSpacing + 10;
            }
            
            StringBuilder contactLine = new StringBuilder();
            if (currentCV.getEmail() != null) contactLine.append(currentCV.getEmail());
            if (currentCV.getPhone() != null && !currentCV.getPhone().isEmpty()) {
                if (contactLine.length() > 0) contactLine.append(" | ");
                contactLine.append(currentCV.getPhone());
            }
            if (currentCV.getAddress() != null && !currentCV.getAddress().isEmpty()) {
                if (contactLine.length() > 0) contactLine.append(" | ");
                contactLine.append(currentCV.getAddress());
            }
            canvas.drawText(contactLine.toString(), margin, yPos, contactPaint);
            yPos += sectionSpacing;
            
            Paint linePaint = new Paint();
            linePaint.setColor(Color.parseColor("#dddddd"));
            linePaint.setStrokeWidth(1);
            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint);
            yPos += sectionSpacing;
            
            String summaryText = currentCV.getSummary();
            if (summaryText != null && !summaryText.isEmpty()) {
                canvas.drawText("PROFESSIONAL SUMMARY", margin, yPos, headerPaint);
                yPos += lineSpacing;
                yPos = drawWrappedText(canvas, summaryText, margin, yPos, pageWidth - 2 * margin, textPaint);
                yPos += sectionSpacing;
            }
            
            List<String> skills = currentCV.getSkills();
            if (skills != null && !skills.isEmpty()) {
                canvas.drawText("SKILLS", margin, yPos, headerPaint);
                yPos += lineSpacing;
                canvas.drawText(String.join(", ", skills), margin, yPos, textPaint);
                yPos += sectionSpacing;
            }
            
            List<String> experience = currentCV.getExperience();
            if (experience != null && !experience.isEmpty() && yPos < pageHeight - margin) {
                canvas.drawText("WORK EXPERIENCE", margin, yPos, headerPaint);
                yPos += lineSpacing;
                for (String exp : experience) {
                    if (exp != null && !exp.trim().isEmpty() && !exp.equals("null")) {
                        yPos = drawWrappedText(canvas, "• " + exp.trim(), margin + 10, yPos, pageWidth - 2 * margin - 10, textPaint);
                        yPos += 5;
                        if (yPos > pageHeight - margin - 50) break;
                    }
                }
                yPos += sectionSpacing - lineSpacing;
            }
            
            List<String> education = currentCV.getEducation();
            if (education != null && !education.isEmpty() && yPos < pageHeight - margin) {
                canvas.drawText("EDUCATION", margin, yPos, headerPaint);
                yPos += lineSpacing;
                for (String edu : education) {
                    if (edu != null && !edu.trim().isEmpty() && !edu.equals("null")) {
                        yPos = drawWrappedText(canvas, "• " + edu.trim(), margin + 10, yPos, pageWidth - 2 * margin - 10, textPaint);
                        yPos += 5;
                        if (yPos > pageHeight - margin - 50) break;
                    }
                }
                yPos += sectionSpacing - lineSpacing;
            }
            
            String linkedin = currentCV.getLinkedinUrl();
            String portfolio = currentCV.getPortfolioUrl();
            if ((linkedin != null && !linkedin.isEmpty()) || (portfolio != null && !portfolio.isEmpty())) {
                canvas.drawText("LINKS", margin, yPos, headerPaint);
                yPos += lineSpacing;
                if (linkedin != null && !linkedin.isEmpty()) {
                    canvas.drawText("LinkedIn: " + linkedin, margin, yPos, textPaint);
                    yPos += lineSpacing;
                }
                if (portfolio != null && !portfolio.isEmpty()) {
                    canvas.drawText("Portfolio: " + portfolio, margin, yPos, textPaint);
                }
            }
            
            document.finishPage(page);
            
            String fileName = currentCV.getTitle() != null ? 
                currentCV.getTitle().replaceAll("[^a-zA-Z0-9\\s]", "").replace(" ", "_") + ".pdf" : 
                "cv_" + System.currentTimeMillis() + ".pdf";
            
            Uri savedUri = null;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                
                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        document.writeTo(outputStream);
                        outputStream.close();
                        savedUri = uri;
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                document.writeTo(fos);
                fos.close();
                savedUri = Uri.fromFile(file);
            }
            
            document.close();
            
            if (savedUri != null) {
                openPdfPreview(savedUri, fileName);
            } else {
                showToast("Error saving PDF");
            }
            
        } catch (Exception e) {
            showToast("Error creating PDF: " + e.getMessage());
        }
    }
    
    private void openPdfPreview(Uri pdfUri, String fileName) {
        Intent intent = new Intent(requireContext(), PdfPreviewActivity.class);
        intent.putExtra("pdfUri", pdfUri);
        intent.putExtra("pdfFileName", fileName);
        startActivity(intent);
    }
    
    private int drawWrappedText(Canvas canvas, String text, int x, int y, int maxWidth, Paint paint) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineSpacing = 16;
        
        for (String word : words) {
            String testLine = line + word + " ";
            float textWidth = paint.measureText(testLine);
            
            if (textWidth > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint);
                y += lineSpacing;
                line = new StringBuilder(word + " ");
            } else {
                line.append(word).append(" ");
            }
        }
        
        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, y, paint);
            y += lineSpacing;
        }
        
        return y;
    }
}
