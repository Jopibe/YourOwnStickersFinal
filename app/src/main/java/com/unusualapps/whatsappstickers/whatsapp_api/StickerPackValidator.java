/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.unusualapps.whatsappstickers.whatsapp_api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;

import com.facebook.animated.webp.WebPImage;
import com.unusualapps.whatsappstickers.utils.FileUtils;
import com.unusualapps.whatsappstickers.utils.ImageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class StickerPackValidator {
    private static final int STICKER_FILE_SIZE_LIMIT_KB = 100;
    private static final int EMOJI_LIMIT = 3;
    private static final int IMAGE_HEIGHT = 512;
    private static final int IMAGE_WIDTH = 512;
    private static final int STICKER_SIZE_MIN = 3;
    private static final int STICKER_SIZE_MAX = 30;
    private static final int CHAR_COUNT_MAX = 128;
    private static final long ONE_KIBIBYTE = 8 * 1024;
    private static final int TRAY_IMAGE_FILE_SIZE_MAX_KB = 50;
    private static final int TRAY_IMAGE_DIMENSION_MIN = 24;
    private static final int TRAY_IMAGE_DIMENSION_MAX = 512;
    private static final String PLAY_STORE_DOMAIN = "play.google.com";
    private static final String APPLE_STORE_DOMAIN = "itunes.apple.com";


    /**
     * Comprueba si un paquete de stickers contiene datos válidos
     */
    static void verifyStickerPackValidity(@NonNull Context context, @NonNull StickerPack stickerPack) throws IllegalStateException {
        if (TextUtils.isEmpty(stickerPack.identifier)) {
            throw new IllegalStateException("El identificador del sticker pack está vacío");
        }
        if (stickerPack.identifier.length() > CHAR_COUNT_MAX) {
            throw new IllegalStateException("El identificador del sticker pack no puede exceder " + CHAR_COUNT_MAX + " caracteres");
        }
        checkStringValidity(stickerPack.identifier);
        if (TextUtils.isEmpty(stickerPack.publisher)) {
            throw new IllegalStateException("El editor del paquete de stickers está vacío, el identificador del paquete de stickers:" + stickerPack.identifier);
        }
        if (stickerPack.publisher.length() > CHAR_COUNT_MAX) {
            throw new IllegalStateException("El publicador del sticker pack no puede exceder " + CHAR_COUNT_MAX + " caracteres, sticker pack identificador:" + stickerPack.identifier);
        }
        if (TextUtils.isEmpty(stickerPack.name)) {
            throw new IllegalStateException("El nombre del sticker pack está vacío, sticker pack identifiador:" + stickerPack.identifier);
        }
        if (stickerPack.name.length() > CHAR_COUNT_MAX) {
            throw new IllegalStateException("El nombre del sticker no puede exceder " + CHAR_COUNT_MAX + " caracteres, sticker pack identificador:" + stickerPack.identifier);
        }
        if (TextUtils.isEmpty(stickerPack.trayImageFile)) {
            throw new IllegalStateException("\n" +
                    "La identificación de la bandeja del paquete de stickers está vacía, el identificador del paquete de stickers:" + stickerPack.identifier);
        }
        if (!TextUtils.isEmpty(stickerPack.androidPlayStoreLink) && !isValidWebsiteUrl(stickerPack.androidPlayStoreLink)) {
            throw new IllegalStateException("\n" +
                    "Asegúrese de incluir http o https en los enlaces de URL, el enlace de Android Play Store no es una URL válida: " + stickerPack.androidPlayStoreLink);
        }
        if (!TextUtils.isEmpty(stickerPack.androidPlayStoreLink) && !isURLInCorrectDomain(stickerPack.androidPlayStoreLink, PLAY_STORE_DOMAIN)) {
            throw new IllegalStateException("\n" +
                    "El enlace de Android Play Store debe usar el dominio de Play Store: " + PLAY_STORE_DOMAIN);
        }
        if (!TextUtils.isEmpty(stickerPack.iosAppStoreLink) && !isValidWebsiteUrl(stickerPack.iosAppStoreLink)) {
            throw new IllegalStateException("\n" +
                    "Asegúrese de incluir http o https en los enlaces de URL, el enlace de la tienda de aplicaciones iOS no es una URL válida:\n " + stickerPack.iosAppStoreLink);
        }
        if (!TextUtils.isEmpty(stickerPack.iosAppStoreLink) && !isURLInCorrectDomain(stickerPack.iosAppStoreLink, APPLE_STORE_DOMAIN)) {
            throw new IllegalStateException("El enlace de la tienda de aplicaciones de iOS debe usar el dominio de la tienda de aplicaciones: " + APPLE_STORE_DOMAIN);
        }
        if (!TextUtils.isEmpty(stickerPack.licenseAgreementWebsite) && !isValidWebsiteUrl(stickerPack.licenseAgreementWebsite)) {
            throw new IllegalStateException("Asegúrese de incluir http o https en los enlaces de URL, el enlace del acuerdo de licencia no es una URL válida:\n " + stickerPack.licenseAgreementWebsite);
        }
        if (!TextUtils.isEmpty(stickerPack.privacyPolicyWebsite) && !isValidWebsiteUrl(stickerPack.privacyPolicyWebsite)) {
            throw new IllegalStateException("\n" +
                    "Asegúrese de incluir http o https en los enlaces de URL, el enlace de la política de privacidad no es una URL válida: " + stickerPack.privacyPolicyWebsite);
        }
        if (!TextUtils.isEmpty(stickerPack.publisherWebsite) && !isValidWebsiteUrl(stickerPack.publisherWebsite)) {
            throw new IllegalStateException("\n" +
                    "Asegúrese de incluir http o https en los enlaces de URL, el enlace del sitio web del editor no es una URL válida: " + stickerPack.publisherWebsite);
        }
        if (!TextUtils.isEmpty(stickerPack.publisherEmail) && !Patterns.EMAIL_ADDRESS.matcher(stickerPack.publisherEmail).matches()) {
            throw new IllegalStateException("\n" +
                    "El correo electrónico del editor no parece válido, el correo electrónico es: " + stickerPack.publisherEmail);
        }
        try {
            InputStream iStream = context.getContentResolver().openInputStream(ImageUtils.getStickerImageAsset(stickerPack.identifier, stickerPack.trayImageFile));
            final byte[] bytes = FileUtils.getBytes(Objects.requireNonNull(iStream));
            if (bytes.length > TRAY_IMAGE_FILE_SIZE_MAX_KB * ONE_KIBIBYTE) {
                throw new IllegalStateException("\n" +
                        "La imagen de la bandeja debe ser inferior a " + TRAY_IMAGE_FILE_SIZE_MAX_KB + " KB, archivo de la imagen de la bandeja: " + stickerPack.trayImageFile);
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap.getHeight() > TRAY_IMAGE_DIMENSION_MAX || bitmap.getHeight() < TRAY_IMAGE_DIMENSION_MIN) {
                throw new IllegalStateException("La altura de la imagen de la bandeja debe estar entre " + TRAY_IMAGE_DIMENSION_MIN + " y " + TRAY_IMAGE_DIMENSION_MAX + " \n" +
                        "píxeles, la altura actual de la imagen de la bandeja es " + bitmap.getHeight() + ", \n" +
                        "archivo de imagen de la bandeja: " + stickerPack.trayImageFile);
            }
            if (bitmap.getWidth() > TRAY_IMAGE_DIMENSION_MAX || bitmap.getWidth() < TRAY_IMAGE_DIMENSION_MIN) {
                throw new IllegalStateException("El ancho de la imagen de la bandeja debe estar entre " + TRAY_IMAGE_DIMENSION_MIN + " y " + TRAY_IMAGE_DIMENSION_MAX + " \n" +
                        "píxeles, el ancho actual de la imagen de la bandeja es " + bitmap.getWidth() + ", \n" +
                        "archivo de imagen de la bandeja: " + stickerPack.trayImageFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se puede abrir la imagen de la bandeja, " + stickerPack.trayImageFile, e);
        }
        final List<Sticker> stickers = stickerPack.getStickers();
        if (stickers.size() < STICKER_SIZE_MIN || stickers.size() > STICKER_SIZE_MAX) {
            throw new IllegalStateException("\n" +
                    "el número de stickers del paquete de stickers debe estar entre 3 y 30 inclusive, actualmente tiene " + stickers.size() + ", \n" +
                    "identificador de paquete de stickers:" + stickerPack.identifier);
        }
        for (final Sticker sticker : stickers) {
            validateSticker(context, stickerPack.identifier, sticker);
        }
    }

    private static void validateSticker(@NonNull Context context, @NonNull final String identifier, @NonNull final Sticker sticker) throws IllegalStateException {
        if (sticker.emojis.size() > EMOJI_LIMIT) {
            throw new IllegalStateException("El recuento de emoji supera el límite, identificador del paquete de pegatinas:" + identifier + ", nombre del archivo:" + sticker.imageFileName);
        }
        if (TextUtils.isEmpty(sticker.imageFileName)) {
            throw new IllegalStateException("\n" +
                    "Sin ruta de archivo para etiqueta, identificador de paquete de etiqueta:" + identifier);
        }
        validateStickerFile(context, identifier, sticker.imageFileName);
    }

    private static void validateStickerFile(@NonNull Context context, @NonNull String identifier, @NonNull final String fileName) throws IllegalStateException {
        try {
            InputStream iStream = context.getContentResolver().openInputStream(ImageUtils.getStickerImageAsset(identifier, fileName));
            final byte[] bytes = FileUtils.getBytes(Objects.requireNonNull(iStream));
            if (bytes.length > STICKER_FILE_SIZE_LIMIT_KB * ONE_KIBIBYTE) {
                throw new IllegalStateException("Sticker debe de ser menor que " + STICKER_FILE_SIZE_LIMIT_KB + "KB, sticker pack identificador:" + identifier + ", \n" +
                        "nombre del archivo:" + fileName);
            }
            try {
                final WebPImage webPImage = WebPImage.create(bytes);
                if (webPImage.getHeight() != IMAGE_HEIGHT) {
                    throw new IllegalStateException("El alto del sticker debe ser " + IMAGE_HEIGHT + ", \n" +
                            "identificador de paquete de pegatinas:" + identifier + ", nombre del archivo:" + fileName);
                }
                if (webPImage.getWidth() != IMAGE_WIDTH) {
                    throw new IllegalStateException("El ancho del sticker debe ser " + IMAGE_WIDTH + ", identificador de paquete de pegatinas:" + identifier + ", nombre del archivo:" + fileName);
                }
                if (webPImage.getFrameCount() > 1) {
                    throw new IllegalStateException("El sticker debería de ser una imagen estática, no hay soporte para stickers animados en este momento, identificador del paquete de stickers:" + identifier + ", nombre del archivo:" + fileName);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("\n" +
                        "Error al analizar la imagen webp, identificador del paquete de stickers:" + identifier + ", nombre del archivo:" + fileName, e);
            }
        } catch (IOException e) {
            throw new IllegalStateException("\n" +
                    "No se puede abrir el archivo de stickers: identificador del paquete de stickers:" + identifier + ", nombre del archivo:" + fileName, e);
        }
    }

    private static void checkStringValidity(@NonNull String string) {
        String pattern = "[\\w-.,'\\s]+"; // [a-zA-Z0-9_-.' ]
        if (!string.matches(pattern)) {
            throw new IllegalStateException(string + " Contiene caracteres no válidos, los caracteres permitidos son de la a a la z, de la A a la Z, _ , ' - . y carácter de espacio");
        }
        if (string.contains("..")) {
            throw new IllegalStateException(string + " no puede contener ..");
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isValidWebsiteUrl(String websiteUrl) throws IllegalStateException {
        try {
            new URL(websiteUrl);
        } catch (MalformedURLException e) {
            Log.e("StickerPackValidator", "url: " + websiteUrl + " está malformado");
            throw new IllegalStateException("url: " + websiteUrl + " está malformado", e);
        }
        return URLUtil.isHttpUrl(websiteUrl) || URLUtil.isHttpsUrl(websiteUrl);

    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isURLInCorrectDomain(String urlString, String domain) throws IllegalStateException {
        try {
            URL url = new URL(urlString);
            if (domain.equals(url.getHost())) {
                return true;
            }
        } catch (MalformedURLException e) {
            Log.e("StickerPackValidator", "url: " + urlString + " está malformado");
            throw new IllegalStateException("url: " + urlString + " está malformado");
        }
        return false;
    }
}
