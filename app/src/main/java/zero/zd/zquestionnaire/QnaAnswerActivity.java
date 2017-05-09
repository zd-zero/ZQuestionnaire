package zero.zd.zquestionnaire;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import zero.zd.zquestionnaire.model.QnA;

public class QnaAnswerActivity extends AppCompatActivity {

    private static final String TAG = QnaAnswerActivity.class.getSimpleName();

    private static final String EXTRA_IS_MISTAKE_LOADED = "EXTRA_IS_MISTAKE_LOADED";

    private static final String SAVED_QNA_INDEX = "SAVED_QNA_INDEX";
    private static final String SAVED_ANSWER_LOCATION_INDEX = "SAVED_ANSWER_LOCATION_INDEX";
    private static final String SAVED_CORRECT_ANSWER = "SAVED_CORRECT_ANSWER";
    private static final String SAVED_MISTAKE_ANSWER = "SAVED_MISTAKE_ANSWER";
    private static final String SAVED_IS_INITIALIZED = "SAVED_IS_INITIALIZED";

    RadioGroup mRadioGroup;
    Button mOkButton;
    TextView mTextQuestion;

    private ArrayList<QnA> mQnAList;
    private ArrayList<QnA> mMistakeQnaList;
    private int mQnAIndex;
    private int mAnswerLocationIndex;
    private int mCorrect;
    private int mMistake;
    private boolean isInitialized;

    public static Intent getStartIntent(Context context, boolean isMistakesLoaded) {
        Intent intent = new Intent(context, QnaAnswerActivity.class);
        intent.putExtra(EXTRA_IS_MISTAKE_LOADED, isMistakesLoaded);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qna_answer);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_qna);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mOkButton = (Button) findViewById(R.id.btn_ok);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (isInitialized)
                    mOkButton.setEnabled(true);
            }
        });
        mTextQuestion = (TextView) findViewById(R.id.text_question);

        mQnAList = new ArrayList<>();
        mMistakeQnaList = new ArrayList<>();

        // check if mistake is loaded
        boolean isMistakesLoaded = getIntent()
                .getBooleanExtra(EXTRA_IS_MISTAKE_LOADED, false);
        if (isMistakesLoaded) {
            mMistakeQnaList = QnaState.getInstance().getMistakeQnaList();
            mQnAList = new ArrayList<>(mMistakeQnaList);
            QnaState.getInstance().setQnAList(mQnAList);
            Log.d(TAG, "mQnAList Size: " + mQnAList.size());
            mMistakeQnaList.clear();
            Log.d(TAG, "Clean mistakeList");
            Log.d(TAG, "mQnAList Size: " + mQnAList.size());

            Log.d(TAG, "Mistakes Loaded!");
        }
        // set qna list
        mQnAList = QnaState.getInstance().getQnAList();

        // retrieve saved instances
        if (savedInstanceState != null) {
            mMistakeQnaList = new ArrayList<>();
            mMistakeQnaList = QnaState.getInstance().getMistakeQnaList();

            mQnAIndex = savedInstanceState.getInt(SAVED_QNA_INDEX);
            mAnswerLocationIndex = savedInstanceState.getInt(SAVED_ANSWER_LOCATION_INDEX);
            mCorrect = savedInstanceState.getInt(SAVED_CORRECT_ANSWER);
            mMistake = savedInstanceState.getInt(SAVED_MISTAKE_ANSWER);
            isInitialized = savedInstanceState.getBoolean(SAVED_IS_INITIALIZED);

            updateQuestionText();
            Log.d(TAG, "Activity recreated.");
        } else {
            mQnAIndex = 0;

            initQnA();
            Log.d(TAG, "Activity initialized.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        QnaState.getInstance().setMistakeQnaList(mMistakeQnaList);

        outState.putInt(SAVED_QNA_INDEX, mQnAIndex);
        outState.putInt(SAVED_ANSWER_LOCATION_INDEX, mAnswerLocationIndex);
        outState.putInt(SAVED_CORRECT_ANSWER, mCorrect);
        outState.putInt(SAVED_MISTAKE_ANSWER, mMistake);
        outState.putBoolean(SAVED_IS_INITIALIZED, isInitialized);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qna_answer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                break;

            case R.id.action_reset:
                resetQnA();
                break;

            case R.id.action_quit:
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Methods for to check if answer is correct,
     * and update QnA
     */
    public void onClickOk(View view) {

        // get radio location
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        int selectedRadioButton = radioGroup.indexOfChild(radioGroup
                .findViewById(radioGroup.getCheckedRadioButtonId()));

        if (selectedRadioButton != mAnswerLocationIndex) {
            // add QnA to mistake list
            mMistakeQnaList.add(mQnAList.get(mQnAIndex));

            showMistakeDialog();
            mMistake++;
        } else {
            Snackbar.make(view, R.string.msg_correct, Snackbar.LENGTH_SHORT).show();
            mCorrect++;
            updateQna();
        }
    }

    /**
     * Method to initialize a question and answer,
     * updates GUI and will run on every increment of mQnAIndex
     */
    private void initQnA() {
        updateQuestionText();

        int[] randIndices = new int[3];
        randIndices[0] = getRandomIndex();
        for (int i = 1; i < 3; i++) {
            while (true) {
                int x = getRandomIndex();
                if (!isRandomIndexExists(randIndices, x)) {
                    randIndices[i] = x;
                    break;
                }
            }
        }
        Log.i(TAG, "Random Indices: " + Arrays.toString(randIndices));

        Random random = new Random();
        mAnswerLocationIndex = random.nextInt(4);

        RadioButton btnOne = (RadioButton) findViewById(R.id.radio_one);
        RadioButton btnTwo = (RadioButton) findViewById(R.id.radio_two);
        RadioButton btnThree = (RadioButton) findViewById(R.id.radio_three);
        RadioButton btnFour = (RadioButton) findViewById(R.id.radio_four);

        ArrayList<RadioButton> radioList = new ArrayList<>();
        radioList.add(btnOne);
        radioList.add(btnTwo);
        radioList.add(btnThree);
        radioList.add(btnFour);

//        Log.i(TAG, "AnswerLoc: " + mAnswerLocationIndex);
        radioList.get(mAnswerLocationIndex).setText(mQnAList.get(mQnAIndex).getAnswer());
        int randIndex = 0;
        for (int i = 0; i < 4; i++) {
            if (i == mAnswerLocationIndex)
                continue;
            radioList.get(i).setText(mQnAList.get(randIndices[randIndex]).getAnswer());
            randIndex++;
        }

        TextView txtProgress = (TextView) findViewById(R.id.text_progress);
        txtProgress.setText(String.format(getResources().getString(R.string.msg_progress),
                mQnAIndex + 1, mQnAList.size(), mCorrect, mMistake));
        mRadioGroup.clearCheck();
        isInitialized = true;
    }

    /**
     * Checks if the answer index generated from {@code getRandomIndex()}
     * if the index already exists on the generated index on array
     * {@code randIndices} for 3 invalid answers
     *
     * @param arr    the array of 3 index of invalid answers to check
     * @param target the newly generated index to compare to {@code arr}
     * @return {@code true} if the array randIndices already contains the
     * newly generated index of answer
     * {@code false} no same index or answer already existed at {@code randIndices}
     * @see #getRandomIndex()
     */
    private boolean isRandomIndexExists(int[] arr, int target) {
        for (int x : arr)
            if (x == target
                    || mQnAList.get(x).getAnswer()
                    .equalsIgnoreCase(mQnAList.get(target).getAnswer()))
                return true;
        return false;
    }

    /**
     * Generates an invalid random answer index and returns an index
     * which is not the same as the answer
     *
     * @return random index
     */
    private int getRandomIndex() {
        Random random = new Random();
        while (true) {
            int x = random.nextInt(mQnAList.size());
            if (x != mQnAIndex && !mQnAList.get(x).getAnswer()
                    .equalsIgnoreCase(mQnAList.get(mQnAIndex).getAnswer())) return x;
        }
    }

    /**
     * Resets the states of the variables, for resetting QnA
     */
    private void resetQnA() {
        mQnAIndex = 0;
        mCorrect = 0;
        mMistake = 0;

        initQnA();

        Snackbar.make(getWindow().getDecorView().getRootView(),
                R.string.msg_reset, Snackbar.LENGTH_SHORT).show();
    }

    private void updateQna() {
        mQnAIndex++;
        if (mQnAIndex == mQnAList.size()) {
            // update mistake list
            QnaState.getInstance().setMistakeQnaList(mMistakeQnaList);

            // get passing
            String assessment = "Failed!";
            int passingCorrectPoints = mQnAList.size() / 2;
            if (mCorrect >= passingCorrectPoints)
                assessment = "Passed!";

            startActivity(QnaResultActivity
                    .getStartIntent(this, assessment, mCorrect));
            return;
        }

        initQnA();
        mOkButton.setEnabled(false);
    }

    private void updateQuestionText() {
        mTextQuestion.setText(mQnAList.get(mQnAIndex).getQuestion());
    }

    private void showMistakeDialog() {
        String msg = "Correct Answer: \n" + mQnAList.get(mQnAIndex).getAnswer();
        new AlertDialog.Builder(QnaAnswerActivity.this)
                .setTitle(R.string.msg_mistake)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateQna();
                        dialog.dismiss();
                    }
                })
                .show();
    }

}
