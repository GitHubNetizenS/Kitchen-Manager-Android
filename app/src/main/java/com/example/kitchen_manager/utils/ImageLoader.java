package com.example.kitchen_manager.utils;

import android.widget.ImageView;
import com.example.kitchen_manager.R;
import com.squareup.picasso.Picasso;

public class ImageLoader {

    private static final String BASE_URL = "http://10.68.169.201:8080";

    public static void loadImage(String imageUrl, ImageView imageView) {
        String fullUrl = getFullUrl(imageUrl);
        Picasso.get()
                .load(fullUrl)
                .placeholder(R.drawable.ic_logo_orange)
                .error(R.drawable.ic_logo_orange)
                .into(imageView);
    }

    private static String getFullUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        // 已经是完整URL
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        // 相对路径，拼接完整URL
        return BASE_URL + imageUrl;
    }
}