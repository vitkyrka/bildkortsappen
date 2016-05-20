/*
 * Copyright (c) 2016 Rabin Vincent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.rab.bildkort;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.InputStream;

class Model {
    public static final String NAME = "in.rab.bildkort";
    public static final String[] FIELDS = {"Images", "Sentences", "Back"};
    public static final String[] CARD_NAMES = {"Card 1"};
    static final String[] AFMT = {"{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}"};

    public static String[] getQuestionFormats(Context context) {
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("model.question.html");
            return new String[]{Utils.inputStreamToString(is)};
        } catch (java.io.IOException e) {
            return new String[]{"Error"};
        }
    }
}
