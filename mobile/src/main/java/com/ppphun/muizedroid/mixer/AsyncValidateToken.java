/*
 * This file is part of Muizedroid's Muizenmixer
 *
 * based upon Ampdroid by
 *
 * Peter Papp
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
import android.os.AsyncTask;
import android.os.SystemClock;

import static com.pppphun.muizedroid.Mixer.ConnectionStatus.CONNECTION_NONE;
import static com.pppphun.muizedroid.Mixer.ConnectionStatus.CONNECTION_UNKNOWN;


class AsyncValidateToken extends AsyncTask<Void, Void, Boolean>
{
    private String authToken;
    private String url;

    private String errorMessage = "";


    AsyncValidateToken(String authToken, String url)
    {
        this.authToken = authToken;
        this.url       = url;
    }


    @Override
    protected final Boolean doInBackground(Void... params)
    {
        // just to be on the safe side
        if ((authToken == null) || authToken.isEmpty()) {
            errorMessage = Mixer.getAppContext().getString(R.string.error_blank_token);
            return false;
        }
        if ((url == null) || url.isEmpty()) {
            errorMessage = Mixer.getAppContext().getString(R.string.error_invalid_server_url);
            return false;
        }

        // can't do anything without network connection
        long                     checkStart       = System.currentTimeMillis();
        Mixer.ConnectionStatus connectionStatus = Mixer.getConnectionStatus();
        while (!isCancelled() && ((connectionStatus == CONNECTION_UNKNOWN) || (connectionStatus == CONNECTION_NONE))) {
            Intent intent = new Intent();
            intent.putExtra("elapsedMS", System.currentTimeMillis() - checkStart);
            Mixer.sendLocalBroadcast(R.string.async_no_network_broadcast_action, intent);

            SystemClock.sleep(1000);

            connectionStatus = Mixer.getConnectionStatus();
        }

        // instantiate Ampache API interface - this handles network operations and XML parsing
        AmpacheAPICaller ampacheAPICaller = new AmpacheAPICaller(url);
        if (!ampacheAPICaller.getErrorMessage().isEmpty()) {
            errorMessage = ampacheAPICaller.getErrorMessage();
            return false;
        }

        // check if token is valid
        boolean tokenValid = ampacheAPICaller.tokenTest(authToken);
        if (!ampacheAPICaller.getErrorMessage().isEmpty()) {
            errorMessage = ampacheAPICaller.getErrorMessage();
            return false;
        }

        return tokenValid;
    }


    @Override
    protected void onPostExecute(Boolean asyncResult)
    {
        // send IPC with results to service
        final Intent intent = new Intent();
        intent.putExtra(Mixer.getAppContext().getString(R.string.async_finished_broadcast_type), Mixer.getAppContext().getResources().getInteger(R.integer.async_validate_token));
        intent.putExtra("isTokenValid", asyncResult);
        intent.putExtra("errorMessage", errorMessage);
        Mixer.sendLocalBroadcast(R.string.async_finished_broadcast_action, intent);
    }
}