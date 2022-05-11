/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.unusualapps.whatsappstickers.whatsapp_api;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ContentFileParser {

    private static final int LIMIT_EMOJI_COUNT = 2;

    @NonNull
    public static List<StickerPack> parseStickerPacks(@NonNull InputStream contentsInputStream) throws IOException, IllegalStateException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(contentsInputStream))) {
            return readStickerPacks(reader);
        }
    }

    @NonNull
    private static List<StickerPack> readStickerPacks(@NonNull JsonReader reader) throws IOException, IllegalStateException {
        List<StickerPack> stickerPackList = new ArrayList<>();
        String androidPlayStoreLink = null;
        String iosAppStoreLink = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if ("androidPlayStoreLink".equals(key)) {
                androidPlayStoreLink = reader.nextString();
            } else if ("iosAppStoreLink".equals(key)) {
                iosAppStoreLink = reader.nextString();
            } else if ("stickerPacks".equals(key)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    StickerPack stickerPack = readStickerPack(reader);
                    stickerPackList.add(stickerPack);
                }
                reader.endArray();
            } else {
                throw new IllegalStateException("campo desconocido en json: " + key);
            }
        }
        reader.endObject();
        if (stickerPackList.size() == 0) {
            throw new IllegalStateException("La lista de stickers no puede estar vacía");
        }
        for (StickerPack stickerPack : stickerPackList) {
            stickerPack.setAndroidPlayStoreLink(androidPlayStoreLink);
            stickerPack.setIosAppStoreLink(iosAppStoreLink);
        }
        return stickerPackList;
    }

    @NonNull
    private static StickerPack readStickerPack(@NonNull JsonReader reader) throws IOException, IllegalStateException {
        reader.beginObject();
        String identifier = null;
        String name = null;
        String publisher = null;
        String trayImageFile = null;
        String publisherEmail = null;
        String publisherWebsite = null;
        String privacyPolicyWebsite = null;
        String licenseAgreementWebsite = null;
        List<Sticker> stickerList = null;
        while (reader.hasNext()) {
            String key = reader.nextName();
            switch (key) {
                case "identifier":
                    identifier = reader.nextString();
                    break;
                case "name":
                    name = reader.nextString();
                    break;
                case "publisher":
                    publisher = reader.nextString();
                    break;
                case "trayImageFile":
                    trayImageFile = reader.nextString();
                    break;
                case "publisherEmail":
                    publisherEmail = reader.nextString();
                    break;
                case "publisherWebsite":
                    publisherWebsite = reader.nextString();
                    break;
                case "privacyPolicyWebsite":
                    privacyPolicyWebsite = reader.nextString();
                    break;
                case "licenseAgreementWebsite":
                    licenseAgreementWebsite = reader.nextString();
                    break;
                case "stickers":
                    stickerList = readStickers(reader);
                    break;
                default:
                    reader.skipValue();
            }
        }
        if (TextUtils.isEmpty(identifier)) {
            throw new IllegalStateException("Identificador no puede estar vacío");
        }
        if (TextUtils.isEmpty(name)) {
            throw new IllegalStateException("Nombre no puede estar vacío");
        }
        if (TextUtils.isEmpty(publisher)) {
            throw new IllegalStateException("Publicador no puede estar vacío");
        }
        if (TextUtils.isEmpty(trayImageFile)) {
            throw new IllegalStateException("tray_image_file no puede estar vacío");
        }
        if (stickerList == null || stickerList.size() == 0) {
            throw new IllegalStateException("La lista de stickers está vacía");
        }
        if (identifier.contains("..") || identifier.contains("/")) {
            throw new IllegalStateException("El identificador no debe contener .. o / para evitar el cruce de directorios");
        }
        reader.endObject();
        final StickerPack stickerPack = new StickerPack(identifier, name, publisher, trayImageFile, publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite);
        stickerPack.setStickers(stickerList);
        return stickerPack;
    }

    @NonNull
    private static List<Sticker> readStickers(@NonNull JsonReader reader) throws IOException, IllegalStateException {
        reader.beginArray();
        List<Sticker> stickerList = new ArrayList<>();

        while (reader.hasNext()) {
            reader.beginObject();
            String imageFile = null;
            List<String> emojis = new ArrayList<>(LIMIT_EMOJI_COUNT);
            while (reader.hasNext()) {
                final String key = reader.nextName();
                if ("imageFileName".equals(key)) {
                    imageFile = reader.nextString();
                } else if ("emojis".equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        String emoji = reader.nextString();
                        emojis.add(emoji);
                    }
                    reader.endArray();
                } else {
                    //throw new IllegalStateException("unknown field in json: " + key);
                    reader.skipValue();
                }
            }
            reader.endObject();
            if (TextUtils.isEmpty(imageFile)) {
                throw new IllegalStateException("Sticker image_file no puede estar vacío");
            }
            if (!imageFile.endsWith(".webp")) {
                throw new IllegalStateException("\n" +
                        "El archivo de imagen para los stickers debe ser un archivo webp, el archivo de imagen es: " + imageFile);
            }
            if (imageFile.contains("..") || imageFile.contains("/")) {
                throw new IllegalStateException("\n" +
                        "El nombre del archivo no debe contener .. o / para evitar el recorrido del directorio, el archivo de imagen es:" + imageFile);
            }
            stickerList.add(new Sticker(imageFile, emojis));
        }
        reader.endArray();
        return stickerList;
    }
}
