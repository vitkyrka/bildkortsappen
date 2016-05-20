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

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;

import org.json.JSONArray;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

public class CardActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String ANKI_DECK_NAME = "Bildkort";
    private SentenceFragment mSentenceFragment;
    private ImageFragment mImageFragment;
    private DefinitionFragment mDefinitonFragment;
    private AddContentApi mApi;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finishCreate();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.anki_needed)
                    .setMessage(R.string.anki_needed_long)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });

            dialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApi = new AddContentApi(this);

        Intent intent = getIntent();

        String word = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (word == null) {
            word = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (word == null) {
            finish();
            return;
        }

        setTitle(word);

        String definition = intent.getStringExtra(Intent.EXTRA_HTML_TEXT);
        Boolean isHtml = false;
        if (definition == null) {
            definition = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (definition == null) {
                definition = word;
            }
        } else {
            isHtml = true;
        }

        String pos = intent.getStringExtra("in.rab.extra.pos");
        if (pos == null) {
            pos = SentenceFragment.POS_UNKNOWN;
        }

        mSentenceFragment = SentenceFragment.newInstance(word, pos);
        mImageFragment = ImageFragment.newInstance(word);
        mDefinitonFragment = DefinitionFragment.newInstance(definition, isHtml);

        if (ContextCompat.checkSelfPermission(this, FlashCardsContract.READ_WRITE_PERMISSION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{FlashCardsContract.READ_WRITE_PERMISSION},
                    0);
        } else {
            finishCreate();
        }
    }

    private class CardFragmentPagerAdapter extends FragmentPagerAdapter {
        public CardFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return mImageFragment;
                case 1:
                    return mSentenceFragment;
                case 2:
                    return mDefinitonFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.images);
                case 1:
                    return getString(R.string.sentences);
                case 2:
                    return getString(R.string.definition);
                default:
                    return null;
            }
        }
    }

    private void finishCreate() {
        setContentView(R.layout.activity_card);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        assert viewPager != null;
        viewPager.setOffscreenPageLimit(10);
        viewPager.setAdapter(new CardFragmentPagerAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    private Long getDeckId() {
        Map<Long, String> deckList = mApi.getDeckList();
        if (deckList != null) {
            for (Map.Entry<Long, String> entry : deckList.entrySet()) {
                if (entry.getValue().equals(ANKI_DECK_NAME)) {
                    return entry.getKey();
                }
            }
        }

        return mApi.addNewDeck(ANKI_DECK_NAME);
    }

    private Long getModelId() {
        Map<Long, String> modelList = mApi.getModelList();
        if (modelList != null) {
            for (Map.Entry<Long, String> entry : modelList.entrySet()) {
                if (entry.getValue().equals(Model.NAME)) {
                    return entry.getKey();
                }
            }
        }

        return mApi.addNewCustomModel(Model.NAME, Model.FIELDS, Model.CARD_NAMES,
                Model.getQuestionFormats(this),
                Model.AFMT, null, null, null);
    }

    private void createCard() {
        ArrayList<String> images = mImageFragment.getSelected();
        ArrayList<String> sentences = mSentenceFragment.getSelected();

        String[] fields = {
                new JSONArray(images).toString(),
                new JSONArray(sentences).toString(),
                mDefinitonFragment.getText()
        };

        Long id = mApi.addNote(getModelId(), getDeckId(), fields, null);
        Toast.makeText(this, id == null ? getString(R.string.failed_add_card) : getString(R.string.card_added),
                Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create:
                createCard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
