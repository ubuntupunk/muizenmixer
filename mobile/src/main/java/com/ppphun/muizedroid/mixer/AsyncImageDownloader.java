/*
 * This file is part of Muizedroid's MuizenMixer
 *
 * based upon Amproid
 *
 by Peter Papp
 *
 * Please visit https://github.com/ubuntupunk/muizenmixer for details
 *
 * Muizedroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Muizedroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Muizedroid. If not, see http://www.gnu.org/licenses/
 */

package com.ppphun.muizedroid.mixer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


final class AsyncImageDownloader extends AsyncTask<Void, Void, Bitmap>
{
    private URL url;


    AsyncImageDownloader(URL url)
    {
        this.url = url;
    }


    @Override
    protected final Bitmap doInBackground(Void... params)
    {
        // download and decode image
        Bitmap largeIcon = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            largeIcon = BitmapFactory.decodeStream(connection.getInputStream());
        }
        catch (IOException e) {
            // nothing to do here, largeIcon remains null
        }

        return largeIcon;
    }


    @Override
    protected void onPostExecute(Bitmap bitmap)
    {
        // handle failure
        if (bitmap == null) {
            return;
        }

        // send IPC to our service
        final Intent intent = new Intent();
        intent.putExtra(Mixer.getAppContext().getString(R.string.async_finished_broadcast_type), Mixer.getAppContext().getResources().getInteger(R.integer.async_image_downloader));
        intent.putExtra("image", bitmap);
        intent.putExtra("url", url.toString());
        Mixer.sendLocalBroadcast(R.string.async_finished_broadcast_action, intent);
    }
}