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

import com.google.gson.annotations.SerializedName;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;


public class SentenceFragment extends ListFragment {
    // This is almost all the corpora classified under Akademiska texter, Skönlitteratur,
    // Tidningstexter, Tidskrifter and Myndighetstexter in the Korp web interface.  Shortening
    // the list greatly improves the response time from Korp, but we of course don't
    // want to miss out on sentences for rare words.
    private static final String CORPORA = "sweachum,sweacsam,aspacsv,romi,romii,rom99,storsuc," +
            "romg,gp1994,gp2001,gp2002,gp2003,gp2004,gp2005,gp2006," +
            "gp2007,gp2008,gp2009,gp2010,gp2011,gp2012,gp2013,gp2d," +
            "press95,press96,press97,press98,webbnyheter2001,webbnyheter2002," +
            "webbnyheter2003,webbnyheter2004,webbnyheter2005,webbnyheter2006," +
            "webbnyheter2007,webbnyheter2008,webbnyheter2009,webbnyheter2010," +
            "webbnyheter2011,webbnyheter2012,webbnyheter2013,attasidor," +
            "dn1987,fof,klarsprak,sou,sfs";

    // https://spraakbanken.gu.se/swe/forskning/saldo/taggm%C3%A4ngd
    public static final String POS_ADJECTIVE = "av.*";
    public static final String POS_ADVERB = "ab.*";
    public static final String POS_INTERJECTION = "in.*";
    public static final String POS_NOUN = "n.*";
    public static final String POS_PREPOSITION = "pp.*";
    public static final String POS_VERB = "vb.*";
    public static final String POS_UNKNOWN = ".*";
    public static final String POS_NONE = "";
    private KorpService mKorp;
    private String mWord;
    private String mPos;
    public static final String API_URL = "https://ws.spraakbanken.gu.se/";
    private ArrayList<String> mSentences;

    public static SentenceFragment newInstance(String word, String pos) {
        SentenceFragment fragment = new SentenceFragment();

        Bundle args = new Bundle();
        args.putString("word", word);
        args.putString("pos", pos);
        fragment.setArguments(args);

        return fragment;
    }

    public static SentenceFragment newInstance(String word) {
        return newInstance(word, POS_UNKNOWN);
    }

    private String getWord() {
        return getArguments().getString("word");
    }

    private static String tokenJoin(QueryResponse.Kwic kwic) {
        StringBuilder sentence = new StringBuilder();
        String delimiter = "";
        String previous = "";
        int quotes = 0;

        for (int i = 0; i < kwic.tokens.size(); i++) {
            String word = kwic.tokens.get(i).toString();

            if (word.equals("\"")) {
                quotes++;
            }

            if (!word.equals(",")
                    && !word.equals(".")
                    && !word.equals(":")
                    && !previous.equals("(")
                    && !word.equals(")")
                    && (!previous.equals("\"") || quotes % 2 == 0)
                    && (!word.equals("\"") || quotes % 2 != 0)) {
                sentence.append(delimiter);
            }

            if (i >= kwic.match.start && i < kwic.match.end) {
                sentence.append("<strong>");
                sentence.append(word);
                sentence.append("</strong>");
            } else {
                sentence.append(word);
            }

            previous = word;
            delimiter = " ";
        }

        return sentence.toString();
    }

    private static ArrayList<String> parseJson(QueryResponse p) {
        ArrayList<String> sentences = new ArrayList<>();

        if (p.kwic == null) {
            return sentences;
        }

        for (QueryResponse.Kwic kwic : p.kwic) {
            sentences.add(tokenJoin(kwic));
        }

        return sentences;
    }

    private class QueryResponse {
        private ArrayList<Kwic> kwic;
        private int hits;

        private class Kwic {
            private ArrayList<Token> tokens;
            private Match match;

            private class Token {
                private String word;

                public String toString() {
                    return word;
                }
            }

            private class Match {
                private int start;
                private int end;
            }
        }
    }

    private class AutoCompleteResponse {
        private Hits hits;

        private class Hits {
            private ArrayList<Hit> hits;
            private int total;

            private class Hit {
                @SerializedName("_source")
                private Source source;

                private class Source {
                    @SerializedName("FormRepresentations")
                    private ArrayList<FormRepresentations> formRepresentations;

                    private class FormRepresentations {
                        String lemgram;
                    }
                }
            }
        }
    }

    public interface KorpService {
        @GET("/ws/korp/?command=query&defaultcontext=1+sentence")
        Call<QueryResponse> query(
                @Query("corpus") String corpus,
                @Query("cqp") String cqp,
                @Query("sort") String sort,
                @Query("start") Integer start,
                @Query("end") Integer end
        );

        @GET("/ws/karp/v1/autocomplete?resource=saldom")
        Call<AutoCompleteResponse> autoComplete(
                @Query("q") String query
        );
    }

    private class KorpCallback implements Callback<QueryResponse> {
        @Override
        public void onResponse(Call<QueryResponse> call,
                Response<QueryResponse> response) {
            if (!response.isSuccessful()) {
                return;
            }

            QueryResponse queryResponse = response.body();
            if (queryResponse.hits == 0 && !mPos.equals(POS_NONE)) {
                mPos = POS_NONE;
                getSentences();
                return;
            }

            initList(parseJson(queryResponse));
        }

        @Override
        public void onFailure(Call<QueryResponse> call, Throwable t) {
            Log.e("Blah", t.getMessage());
        }
    }

    private void getSentences() {
        String cqp;

        if (mPos.equals(POS_NONE)) {
            cqp = String.format("\"%s\"", mWord.replace(" ", "\" \""));
        } else {
            cqp = String.format("[lex contains \"%s\\.\\.%s\\.1\"]", mWord.replace(' ', '_'), mPos);
        }

        Call<QueryResponse> call = mKorp.query(CORPORA, cqp, "random", 0, 30);
        call.enqueue(new KorpCallback());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Cache cache = new Cache(getContext().getCacheDir(), 1024 * 1024);

        OkHttpClient client = new OkHttpClient.Builder()
                .cache(cache)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mKorp = retrofit.create(KorpService.class);

        mWord = getArguments().getString("word");
        mPos = getArguments().getString("pos");
        if (!mPos.isEmpty() && !mPos.endsWith("*")) {
            mPos += ".*";
        }

        getSentences();
    }

    private void initList(ArrayList<String> sentences) {
        ArrayList<Spanned> spanneds = new ArrayList<>();

        for (String sentence : sentences) {
            spanneds.add(Html.fromHtml(sentence));
        }

        ArrayAdapter<Spanned> adapter = new ArrayAdapter<>(getActivity(),
                R.layout.simple_list_item_multiple_choice_left,
                spanneds);

        mSentences = sentences;

        ListView listView = getListView();
        setListAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    public ArrayList<String> getSelected() {
        ArrayList<String> selected = new ArrayList<>();
        ListView listView = getListView();
        SparseBooleanArray items = listView.getCheckedItemPositions();

        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i)) {
                continue;
            }

            selected.add(mSentences.get(i).replace("strong>", "em>"));
        }

        return selected;
    }
}
