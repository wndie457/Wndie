package com.maks.otacard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {
    private ImageView preview;
    private TextView status;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p * 2, p, p);
        TextView t = new TextView(this);
        t.setText("Фото для карточки ОТА");
        t.setTextSize(22);
        root.addView(t);
        status = new TextView(this);
        status.setText("Выбери фото, потом открой Настройки → Об устройстве (если не изменилось — закрой Настройки и открой снова).");
        root.addView(status);
        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        Button pick = new Button(this);
        pick.setText("Выбрать фото");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, 1);
        });
        root.addView(pick);
        Button clear = new Button(this);
        clear.setText("Убрать фото");
        clear.setOnClickListener(v -> {
            new File(getFilesDir(), "card.png").delete();
            preview.setImageDrawable(null);
            status.setText("Фото убрано.");
        });
        root.addView(clear);
        setContentView(root);
        File f = new File(getFilesDir(), "card.png");
        if (f.exists()) preview.setImageBitmap(BitmapFactory.decodeFile(f.getPath()));
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req != 1 || res != RESULT_OK || data == null) return;
        try {
            Uri u = data.getData();
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(u)) { BitmapFactory.decodeStream(in, null, o); }
            int s = 1;
            while (o.outWidth / s > 2400) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            Bitmap bm;
            try (InputStream in = getContentResolver().openInputStream(u)) { bm = BitmapFactory.decodeStream(in, null, o); }
            int side = Math.min(bm.getWidth(), bm.getHeight());
            Bitmap sq = Bitmap.createBitmap(bm, (bm.getWidth() - side) / 2, (bm.getHeight() - side) / 2, side, side);
            Bitmap out = Bitmap.createScaledBitmap(sq, 984, 984, true);
            try (FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "card.png"))) {
                out.compress(Bitmap.CompressFormat.PNG, 100, fo);
            }
            preview.setImageBitmap(out);
            status.setText("Готово. Перезапусти Настройки (смахни из недавних) и открой «Об устройстве».");
        } catch (Exception e) {
            status.setText("Ошибка: " + e);
        }
    }
}
