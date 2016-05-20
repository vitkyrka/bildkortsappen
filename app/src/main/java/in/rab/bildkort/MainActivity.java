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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {
    private class PosItem {
        private String mName;
        private String mId;

        public PosItem(int resource, String id) {
            mName = getString(resource);
            mId = id;
        }

        public String getId() {
            return mId;
        }

        public String toString() {
            return mName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText wordEdit = (EditText) findViewById(R.id.wordEdit);
        final Spinner posSpinner = (Spinner) findViewById(R.id.posSpinner);

        PosItem[] array = {
                new PosItem(R.string.pos_unknown, SentenceFragment.POS_UNKNOWN),
                new PosItem(R.string.pos_noun, SentenceFragment.POS_NOUN),
                new PosItem(R.string.pos_adjective, SentenceFragment.POS_ADJECTIVE),
                new PosItem(R.string.pos_verb, SentenceFragment.POS_VERB),
                new PosItem(R.string.pos_preposition, SentenceFragment.POS_PREPOSITION),
                new PosItem(R.string.pos_none, SentenceFragment.POS_NONE),
        };

        ArrayAdapter<PosItem> adapter = new ArrayAdapter<PosItem>(this,
                android.R.layout.simple_spinner_item, array);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        posSpinner.setAdapter(adapter);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CardActivity.class);
                PosItem posItem = (PosItem) posSpinner.getSelectedItem();

                intent.putExtra(Intent.EXTRA_SUBJECT, wordEdit.getText().toString());
                intent.putExtra("in.rab.extra.pos", posItem.getId());
                startActivity(intent);
            }
        });
    }

}
