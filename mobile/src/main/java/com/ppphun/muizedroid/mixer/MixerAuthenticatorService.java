/*
 * This file is part of MuizenDroid's MuizenMixer
 *
 *based upon Amproid by Peter Papp
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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;


public class MixerAuthenticatorService extends Service
{
    private MixerAuthenticator authenticator = null;


    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        if (authenticator == null) {
            authenticator = new MixerAuthenticator(this);
        }

        return authenticator.getIBinder();
    }
}
