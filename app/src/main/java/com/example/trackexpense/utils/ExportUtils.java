package com.example.trackexpense.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.example.trackexpense.data.local.Expense;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportUtils {

    public static File exportToCsv(Context context, List<Expense> expenses, String currencySymbol) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

        String fileName = "expenses_" + fileNameFormat.format(new Date()) + ".csv";
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Date,Category,Type,Amount,Notes\n");

        for (Expense expense : expenses) {
            csvContent.append(dateFormat.format(new Date(expense.getDate()))).append(",");
            csvContent.append(expense.getCategory()).append(",");
            csvContent.append(expense.getType()).append(",");
            csvContent.append(currencySymbol).append(expense.getAmount()).append(",");
            csvContent.append("\"").append(expense.getNotes() != null ? expense.getNotes() : "").append("\"");
            csvContent.append("\n");
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(csvContent.toString().getBytes());
        }

        return file;
    }

    public static void shareFile(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(shareIntent, "Share Expenses"));
    }
}
