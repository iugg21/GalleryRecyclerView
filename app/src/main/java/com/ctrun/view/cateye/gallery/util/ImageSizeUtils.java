package com.ctrun.view.cateye.gallery.util;

import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Created by ctrun on 2017/9/19.
 */

public class ImageSizeUtils {

    public static String makeSmallUrl(ImageView view, String url) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int max = Math.max(lp.height, lp.width);
        if (max > 0) {
            return makeSmallUrlSquare(url, max);
        }

        return url;
    }

    public static boolean canCrop(String url) {
        return url.startsWith("http") && url.contains("imageMogr2/thumbnail/");
    }

    public static String makeSmallUrlSquare(String url, int widthPix) {
        if (canCrop(url)) {
            url = url.substring(0, url.indexOf("imageMogr2/thumbnail") + "imageMogr2/thumbnail".length());
            return String.format(Locale.getDefault(), "%s/%dx%d", url, widthPix, widthPix);
        } else {
            return url;
        }
    }

    public static String makeSmallUrlSquare(String url, int widthPix, String size, String size2) {
        if (canCrop(url)) {
            url = url.substring(0, url.indexOf("imageMogr2/thumbnail") + "imageMogr2/thumbnail".length());
            return String.format(Locale.getDefault(), "%s/%dx%d", url, widthPix, widthPix);
        } else {
            return ImageSizeUtils.makePicUrl(url, size, size2);
        }
    }

    //部分图片通过拼接url
    public static String makePicUrl(@NonNull String url, String size) {
        return ImageSizeUtils.makePicUrl(url, size, null);
    }

    //部分图片通过拼接url
    public static String makePicUrl(@NonNull String url, String size, String size2) {
        if (url.startsWith("https://obj.pipi.cn")) {
            if (size2 != null) {
                return url + size2;
            }

            return url;
        }

        if (url.contains("@")) {
            String origin = url.substring(0, url.indexOf("@"));
            return origin.replace("/w.h/", "/") + size;
        }
        if (!url.contains("/w.h/")) {
            return url + size;
        }

        return url.replace("/w.h/", "/") + size;
    }

    //通过替换w.h获取图片
    public static String processUrl(@NonNull String url, int width, int height) {
        return url.replace("/w.h/", "/" + width + "." + height + "/");
    }

    public static String processUrl(@NonNull String url) {
        if (url.contains("@")) {
            url = url.substring(0, url.indexOf("@"));
        }

        return url.replace("/w.h/", "/");
    }
}
