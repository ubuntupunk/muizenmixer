/*
 * This file is part of Muizedroid's MuizenMixer
 *
 based upon Amproid by Peter Papp
 *
 * Please visit https://github.com/4phun/Muizedroid for details
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import static com.pppphun.muizedroid.Mixer.ConnectionStatus.CONNECTION_NONE;
import static com.pppphun.muizedroid.Mixer.ConnectionStatus.CONNECTION_UNKNOWN;
import static com.pppphun.muizedroid.MixerService.PREFIX_PLAYLIST;


public class AsyncGetPlaylists extends AsyncTask<Void, Void, Vector<HashMap<String, String>>>
{
    private String authToken;
    private String url;

    private MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> resultToSend;

    private String errorMessage = "";


    AsyncGetPlaylists(String authToken, String url, MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> resultToSend)
    {
        this.authToken    = authToken;
        this.url          = url;
        this.resultToSend = resultToSend;
    }


    @Override
    protected final Vector<HashMap<String, String>> doInBackground(Void... params)
    {
        // just to be on the safe side
        if ((authToken == null) || authToken.isEmpty()) {
            errorMessage = Muizedroid.getAppContext().getString(R.string.error_blank_token);
            return new Vector<>();
        }
        if ((url == null) || url.isEmpty()) {
            errorMessage = Muizedroid.getAppContext().getString(R.string.error_invalid_server_url);
            return new Vector<>();
        }

        // can't do anything without network connection
        long                     checkStart       = System.currentTimeMillis();
        Muizedroid.ConnectionStatus connectionStatus = Muizedroid.getConnectionStatus();
        while (!isCancelled() && ((connectionStatus == CONNECTION_UNKNOWN) || (connectionStatus == CONNECTION_NONE))) {
            Intent intent = new Intent();
            intent.putExtra("elapsedMS", System.currentTimeMillis() - checkStart);
            Muizedroid.sendLocalBroadcast(R.string.async_no_network_broadcast_action, intent);

            SystemClock.sleep(1000);

            connectionStatus = Muizedroid.getConnectionStatus();
        }

        // instantiate Ampache API interface - this handles network operations and XML parsing
        AmpacheAPICaller ampacheAPICaller = new AmpacheAPICaller(url);
        if (!ampacheAPICaller.getErrorMessage().isEmpty()) {
            errorMessage = ampacheAPICaller.getErrorMessage();
            return new Vector<>();
        }

        // get the playlists
        Vector<HashMap<String, String>> playlists = ampacheAPICaller.getPlaylists(authToken);
        if (!ampacheAPICaller.getErrorMessage().isEmpty()) {
            errorMessage = ampacheAPICaller.getErrorMessage();
            return new Vector<>();
        }

        return playlists;
    }


    @Override
    protected void onPostExecute(Vector<HashMap<String, String>> playlists)
    {
        ArrayList<MediaBrowserCompat.MediaItem> results = new ArrayList<>();

        // @formatter:off

        // add root a.k.a. link to "home" as the first element
        results.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(Muizedroid.getAppContext().getString(R.string.item_root_id))
                .setTitle(Muizedroid.getAppContext().getString(R.string.item_root_desc))
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));

        if (!errorMessage.isEmpty()) {
            // add another link to "home" with the error message
            results.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(Muizedroid.getAppContext().getString(R.string.item_root_id))
                    .setTitle(errorMessage)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        }

        // @formatter:on

        final SharedPreferences preferences      = Muizedroid.getAppContext().getSharedPreferences(Muizedroid.getAppContext().getString(R.string.options_preferences), Context.MODE_PRIVATE);
        boolean                 hideDotPlaylists = preferences.getBoolean(Muizedroid.getAppContext().getString(R.string.dot_playlists_hide_preference), true);

        // add all resulting playlists
        int defective = 0;
        for(HashMap<String, String> playlist : playlists) {
            // just to be on the safe side
            if (!playlist.containsKey("id") || !playlist.containsKey("name")) {
                defective++;
                continue;
            }

            String name = playlist.get("name");

            // skip .playlist if user prefers
            if (hideDotPlaylists && (name != null) && name.startsWith(".")) {
                continue;
            }

            // @formatter:off
            results.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(PREFIX_PLAYLIST + playlist.get("id"))
                    .setTitle(name)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            // @formatter:on
        }

        // let everyone know if there was a problem
        if (defective > 0) {
            // yet another link to home
            // @formatter:off
            results.add(new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(Muizedroid.getAppContext().getString(R.string.item_root_id))
                    .setTitle(String.format(Muizedroid.getAppContext().getString(R.string.error_defective_playlists), defective))
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
            // @formatter:on
        }

        // send it back to the media browser service and we're done
        resultToSend.sendResult(results);

        // for MixerService to update position
        Mixer.sendLocalBroadcast(R.string.async_finished_broadcast_action, null);
    }
}
