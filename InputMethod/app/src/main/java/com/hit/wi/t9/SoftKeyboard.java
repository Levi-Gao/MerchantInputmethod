package com.hit.wi.t9;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.*;

import com.hit.wi.jni.InitInputParam;
import com.hit.wi.t9.Business.entity.Goods;
import com.hit.wi.util.CommonFuncs;
import com.hit.wi.util.DisplayUtil;
import com.hit.wi.util.InputMode;
import com.hit.wi.jni.*;
import com.hit.wi.t9.Interfaces.SoftKeyboardInterface;
import com.hit.wi.t9.effect.KeyBoardTouchEffect;
import com.hit.wi.t9.functions.GenerateMessage;
import com.hit.wi.t9.functions.PinyinEditProcess;
import com.hit.wi.t9.functions.SharedPreferenceManager;
import com.hit.wi.t9.functions.SymbolsManager;
import com.hit.wi.t9.values.Global;
import com.hit.wi.t9.values.SkinInfoManager;
import com.hit.wi.t9.view.LightViewManager;
import com.hit.wi.t9.view.PreEditPopup;
import com.hit.wi.t9.view.QuickButton;
import com.hit.wi.t9.view.SetKeyboardSizeView;
import com.hit.wi.t9.view.SetKeyboardSizeView.OnChangeListener;
import com.hit.wi.t9.view.SetKeyboardSizeView.SettingType;
import com.hit.wi.t9.viewGroups.*;
import com.umeng.analytics.MobclickAgent;

import java.io.IOException;
import java.util.List;

import static java.lang.Thread.sleep;


public final class SoftKeyboard extends InputMethodService implements SoftKeyboardInterface {


    /**
     * ???????????????????????????????????????
     * ?????????????????????????????????????????????
     */
    static {
        System.loadLibrary("WIIM_NK");
        System.loadLibrary("WIIM");
    }

    /**
     * ????????????
     */
    private String orientation;

    /**
     * ??????????????????
     */
    private final String ORI_HOR = "_H";

    /**
     * ??????????????????
     */
    private final String ORI_VER = "_V";

    /**
     * ????????????????????????????????????
     */
    private boolean mSetKeyboardSizeViewOn = false;

    /**
     * ???????????????Emoji
     */
    private String mQKOrEmoji = Global.QUANPIN;

    /**
     * ??????????????????
     */
    private int zhKeyboard;

    private boolean keyboard_animation_switch;
    /**
     * ????????????
     */
    private int mScreenWidth;

    /**
     * ????????????
     */
    private int mScreenHeight;

    /**
     * ???????????????
     */
    private int mStatusBarHeight;

    public String[] mFuncKeyboardText;

    /**
     * ???????????????????????????
     */
    private static final int DISABLE_LAYOUTPARAMS_FLAG = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

    //?????????FLAG_NOT_FOCUSABLE??????????????????????????????????????????????????????\
    private static final int ABLE_LAYOUTPARAMS_FLAG = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;


    private final int MSG_HIDE = 0;

    //    public static final int SET_ALPHA_VIEW_DESTROY = -803;
    //???????????????

    public final int MSG_REPEAT = 1;

    private final int MSG_SEND_TO_KERNEL = 2;
    private final int QK_MSG_SEND_TO_KERNEL = 3;
    private final int MSG_CHOOSE_WORD = 4;
    private final int MSG_HEART = 5;
    private final int MSG_LAZY_LOAD_CANDIDATE = 6;
    private final int MSG_DOUBLE_CLICK_REFRESH = 7;
    private final int MSG_KERNEL_CLEAN = 8;
    private final int MSG_CLEAR_ANIMATION = 9;
    private final int MSG_REMOVE_INPUT = 10;
    private final int ALPHA_DOWN_TIME = 7;

    private static final int REPEAT_INTERVAL = 50; // ?????????????????????
    public static final int REPEAT_START_DELAY = 400;// ????????????
    private static final short DELAY_TIME_REMOVE = 400;

    //????????????
    private int DEFAULT_FULL_WIDTH;
    private int DEFAULT_FULL_WIDTH_X;
    private String FULL_WIDTH_S = "FULL_WIDTH";
    private String FULL_WIDTH_X_S = "FULL_WIDTH_X";
    private int DEFAULT_KEYBOARD_X;
    private int DEFAULT_KEYBOARD_Y;
    private int DEFAULT_KEYBOARD_WIDTH;
    private int DEFAULT_KEYBOARD_HEIGHT;

    private final String KEYBOARD_X_S = "KEYBOARD_X";
    private final String KEYBOARD_Y_S = "KEYBOARD_Y";
    private final String KEYBOARD_WIDTH_S = "KEYBOARD_WIDTH";
    private final String KEYBOARD_HEIGHT_S = "KEYBOARD_HEIGHT";

    private boolean mWindowShown = false;


    public int keyboardWidth = 0;
    public int keyboardHeight = 0;
    public int standardVerticalGapDistance = 10;
    public int standardHorizontalGapDistance = 0;
    private int maxFreeKernelTime = 60;

    private InitInputParam initInputParam;
    public Typeface mTypeface;

    public LinearLayout keyboardLayout;//???????????????
    public WindowManager.LayoutParams keyboardParams = new WindowManager.LayoutParams();
    public LinearLayout secondLayerLayout;
    private LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    public LinearLayout mInputViewGG;
    public LinearLayout.LayoutParams mGGParams = new LinearLayout.LayoutParams(0, 0);

    public WindowManager.LayoutParams mSetKeyboardSizeParams = new WindowManager.LayoutParams();
    public SetKeyboardSizeView mSetKeyboardSizeView;
    private QuickButton largeCandidateButton;

    private Listeners listeners = new Listeners();
    public KeyBoardSwitcherC keyBoardSwitcher = new KeyBoardSwitcherC();
    public SkinUpdateC skinUpdateC = new SkinUpdateC();
    public ScreenInfoC screenInfoC = new ScreenInfoC();
    public TransparencyHandle transparencyHandle = new TransparencyHandle();
    public ViewManagerC viewManagerC = new ViewManagerC();
    public FunctionsC functionsC = new FunctionsC();
    public ViewSizeUpdateC viewSizeUpdate = new ViewSizeUpdateC();

    public QKInputViewGroup qkInputViewGroup;
    public SpecialSymbolChooseViewGroup specialSymbolChooseViewGroup;
    public FunctionViewGroup functionViewGroup;
    public QuickSymbolViewGroup quickSymbolViewGroup;
    public PreFixViewGroup prefixViewGroup;
    public BottomBarViewGroup bottomBarViewGroup;
    public CandidatesViewGroup candidatesViewGroup;
    public CandidatesViewGroup_goodsName candidatesViewGroup_goodsName;
    public CandidatesViewGroup_goodsInfo candidatesViewGroup_goodsInfo;
    public CandidatesViewGroup_goodsWords candidatesViewGroup_goodsWords;
    public CandidatesViewGroup_words candidatesViewGroup_words;
    public T9InputViewGroup t9InputViewGroup;
    public LightViewManager lightViewManager;
    public PreEditPopup preEditPopup;

    public SymbolsManager symbolsManager;
    public KeyBoardTouchEffect keyboardTouchEffect;
    public SkinInfoManager skinInfoManager;
    public PinyinEditProcess pinyinProc;
    private Resources res;

    /**
     * ??????????????????
     */
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE:
                    if (candidatesViewGroup.isShown()) Global.keyboardRestTimeCount = 0;
                    if (candidatesViewGroup_words.isShown()) Global.keyboardRestTimeCount = 0;
                    if (candidatesViewGroup_goodsName.isShown()) Global.keyboardRestTimeCount = 0;
                    if (candidatesViewGroup_goodsWords.isShown()) Global.keyboardRestTimeCount = 0;
                    if (candidatesViewGroup_goodsInfo.isShown()) Global.keyboardRestTimeCount = 0;
                    if (Global.keyboardRestTimeCount > ALPHA_DOWN_TIME) {
                        if (!transparencyHandle.isUpAlpha) transparencyHandle.UpAlpha();
                        Global.keyboardRestTimeCount = 0;
                    } else {
                        Global.keyboardRestTimeCount++;
                    }
                    mHandler.removeMessages(MSG_HIDE);
                    mHandler.sendEmptyMessageDelayed(MSG_HIDE, Global.metaRefreshTime);
                    break;
                case MSG_REPEAT:
                    deleteLast();
                    sendEmptyMessageDelayed(MSG_REPEAT, REPEAT_INTERVAL);
                    break;
                case MSG_SEND_TO_KERNEL:
                    editPinyin((String) msg.obj, false);
                    t9InputViewGroup.updateFirstKeyText();
                    break;
                case QK_MSG_SEND_TO_KERNEL:
                    editPinyin((String) msg.obj, false);
                    qkInputViewGroup.refreshQKKeyboardPredict();
                    break;
                case MSG_CHOOSE_WORD:
                    chooseWord(msg.arg1);
                    break;
                case MSG_LAZY_LOAD_CANDIDATE:
                    candidatesViewGroup.setCandidates((List<String>) msg.obj);
                    candidatesViewGroup_words.setCandidates((List<String>) msg.obj);
                    candidatesViewGroup_goodsName.setCandidates((List<String>) msg.obj);
                    candidatesViewGroup_goodsWords.setCandidates((List<String>) msg.obj);
                    candidatesViewGroup_goodsInfo.setCandidates((Goods) msg.obj);
                    break;
                case MSG_DOUBLE_CLICK_REFRESH:
                    mHandler.removeMessages(MSG_DOUBLE_CLICK_REFRESH);
                    mHandler.sendEmptyMessageDelayed(MSG_DOUBLE_CLICK_REFRESH, 3 * Global.metaRefreshTime);
                    break;
                case MSG_KERNEL_CLEAN:
                    mHandler.removeMessages(MSG_KERNEL_CLEAN);
                    break;
                case MSG_CLEAR_ANIMATION:
                    clearAnimation();
                    break;
                case MSG_REMOVE_INPUT:
                    viewManagerC.removeInputView();
                    break;
            }

            super.handleMessage(msg);
        }
    };

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????
     * ?????????????????????????????????????????????
     */
    @Override
    public void onCreate() {
        /*
         * ????????????
         */
        Log.i("WIVE", "onCreate");
        initInputParam = new InitInputParam();

        /*
         * ?????????SharedPreferences??????
         */
        SharedPreferenceManager.initSharedPreferencesData(this);//????????????????????????????????????

        iniComponent();

        GenerateMessage gm = new GenerateMessage(this, 1);
        gm.generate();
        screenInfoC.refreshScreenInfo();
        KeyBoardCreate keyBoardCreate = new KeyBoardCreate();
        keyBoardCreate.createKeyboard();
        super.onCreate();
    }

    private void iniComponent() {
        res = getResources();
        keyboardTouchEffect = new KeyBoardTouchEffect(this);
        specialSymbolChooseViewGroup = new SpecialSymbolChooseViewGroup();
        functionViewGroup = new FunctionViewGroup();
        quickSymbolViewGroup = new QuickSymbolViewGroup();
        prefixViewGroup = new PreFixViewGroup();
        bottomBarViewGroup = new BottomBarViewGroup();
        candidatesViewGroup = new CandidatesViewGroup();
        candidatesViewGroup_words = new CandidatesViewGroup_words();
        candidatesViewGroup_goodsName = new CandidatesViewGroup_goodsName();
        candidatesViewGroup_goodsWords = new CandidatesViewGroup_goodsWords();
        candidatesViewGroup_goodsInfo = new CandidatesViewGroup_goodsInfo();
        qkInputViewGroup = new QKInputViewGroup();
        t9InputViewGroup = new T9InputViewGroup();
        preEditPopup = new PreEditPopup();
        lightViewManager = new LightViewManager();
        pinyinProc = new PinyinEditProcess(this);

        secondLayerLayout = new LinearLayout(this);
        keyboardLayout = new LinearLayout(this);
        keyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardLayout.setBackgroundColor(Global.BackgroundColor);
        //keyboardLayout.setPadding( 0,12,  0, 0);
        skinInfoManager = SkinInfoManager.getSkinInfoManagerInstance();
        symbolsManager = new SymbolsManager(this);
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????????????????????????????????
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        screenInfoC.refreshScreenInfo();
        screenInfoC.LoadKeyboardSizeInfoFromSharedPreference();

        viewSizeUpdate.updateViewSizeAndPosition();

        if (mWindowShown) {
            updateWindowManager();
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        pinyinProc.mSelStart = Math.min(newSelStart, newSelEnd);
        pinyinProc.mSelEnd = Math.max(newSelStart, newSelEnd);
        pinyinProc.mCandidateStart = Math.min(candidatesStart, candidatesEnd);
        pinyinProc.mCandidateEnd = Math.max(candidatesStart, candidatesEnd);
        transparencyHandle.handleAlpha(MotionEvent.ACTION_DOWN);
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     * ????????????????????????????????????
     */
    @Override
    public void onDestroy() {
//        Log.i("WIVE", "onDestroy softkeyboard");
        viewManagerC.removeInputView();
//        lightViewManager.removeView();
        Kernel.cleanKernel();
        Kernel.resetWiIme(InitInputParam.RESET);
        Kernel.freeIme();
        super.onDestroy();
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????
     * selstart????????? selection Start ?????????????????????????????????selEnd??????
     * ???????????????{@link #mHandler}??????{@link #MSG_SEND_TO_KERNEL}?????????
     *
     * @param s ????????????????????????,delete ??????????????????
     */
    public void editPinyin(String s, boolean delete) {
        if (pinyinProc.borderEditProcess(s, delete))
            return;// promise candidateStart<selStart<candidateEnd
        mQKOrEmoji = Global.QUANPIN;

        final InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        final String pinyin = Kernel.getWordsShowPinyin();
        if (pinyin.length() != pinyinProc.mCandidateEnd - pinyinProc.mCandidateStart) return;

        //???????????????,???????????????????????????????????????????????????tm????????????????????????????????????
        final int isDel = delete && pinyinProc.mSelStart == pinyinProc.mSelEnd ? 1 : 0;
        int cursorBefore = pinyinProc.mSelStart - pinyinProc.mCandidateStart - isDel;
        String sBefore = cursorBefore < pinyin.length() ? pinyin.substring(0, cursorBefore) : pinyin;
        sBefore = sBefore.replace("'", "") + s;
        String sAfter = pinyinProc.mSelEnd <= pinyinProc.mCandidateStart ? pinyin :
                (pinyinProc.mSelEnd >= pinyinProc.mCandidateEnd ? "" :
                        pinyin.substring(pinyinProc.mSelEnd - pinyinProc.mCandidateStart).replace("'", ""));
        pinyinProc.innerEditProcess(ic, sBefore, s, sAfter, delete);
    }

    /**
     * ????????????????????????????????????
     * ????????????????????????????????????????????????????????????
     */
    public void updateSetKeyboardSizeViewPos() {
        Rect keyboardRect = new Rect(keyboardParams.x, keyboardParams.y,
                keyboardParams.x + keyboardParams.width,
                keyboardParams.y + keyboardParams.height);
        mSetKeyboardSizeView.SetScreenInfo(mScreenWidth, mScreenHeight, mStatusBarHeight);
        mSetKeyboardSizeView.SetPos(keyboardRect);
    }

    private boolean lastHideState = false;

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????????????????????
     */
    public void refreshDisplay(boolean special) {
        boolean isNK = Global.currentKeyboard == Global.KEYBOARD_T9 || Global.currentKeyboard == Global.KEYBOARD_SYM;
        boolean hideCandidate = Kernel.getWordsNumber() == 0 && !special;
        Kernel.setKernelType(isNK ? Kernel.NINE_KEY : Kernel.QWERT);

        if (mInputViewGG.isShown()) mInputViewGG.setVisibility(View.VISIBLE);
        functionViewGroup.refreshState(hideCandidate);
        functionsC.refreshStateForSecondLayout();
        functionsC.refreshStateForLargeCandidate(hideCandidate);
        if (lastHideState != hideCandidate && !hideCandidate) {
            viewSizeUpdate.UpdatePreEditSize();
            viewSizeUpdate.UpdateCandidateSize();
            viewSizeUpdate.UpdateLargeCandidateSize();
        }

        candidatesViewGroup.refreshState(hideCandidate, isNK ? Global.EMOJI : Global.QUANPIN);
        candidatesViewGroup_words.hide();
        candidatesViewGroup_goodsName.hide();
        candidatesViewGroup_goodsWords.hide();
        candidatesViewGroup_goodsInfo.hide();
        specialSymbolChooseViewGroup.refreshState(hideCandidate);
        prefixViewGroup.refreshState();
        t9InputViewGroup.refreshState();
        qkInputViewGroup.refreshState();
        quickSymbolViewGroup.refreshState();
        viewSizeUpdate.UpdateQuickSymbolSize();
        bottomBarViewGroup.refreshState();
        preEditPopup.refreshState();
    }

    /**
     * ????????????????????????
     * */

    public void refreshDisplay_Words(boolean special) {
        boolean isNK = Global.currentKeyboard == Global.KEYBOARD_T9 || Global.currentKeyboard == Global.KEYBOARD_SYM;
        boolean hideCandidate = Kernel.getWordsNumber() == 0 && !special;
        Kernel.setKernelType(isNK ? Kernel.NINE_KEY : Kernel.QWERT);

        if (mInputViewGG.isShown()) mInputViewGG.setVisibility(View.VISIBLE);
        functionViewGroup.refreshState(hideCandidate);
        functionsC.refreshStateForSecondLayout();
        functionsC.refreshStateForLargeCandidate(hideCandidate);
        if (lastHideState != hideCandidate && !hideCandidate) {
            viewSizeUpdate.UpdatePreEditSize();
            viewSizeUpdate.UpdateCandidateSize();
            viewSizeUpdate.UpdateLargeCandidateSize();
        }

        candidatesViewGroup.hide();
        candidatesViewGroup_words.refreshState(hideCandidate, isNK ? Global.EMOJI : Global.QUANPIN);
        candidatesViewGroup_goodsName.hide();
        candidatesViewGroup_goodsWords.hide();
        candidatesViewGroup_goodsInfo.hide();
        specialSymbolChooseViewGroup.refreshState(hideCandidate);
        prefixViewGroup.refreshState();
        t9InputViewGroup.refreshState();
        qkInputViewGroup.refreshState();
        quickSymbolViewGroup.refreshState();
        viewSizeUpdate.UpdateQuickSymbolSize();
        bottomBarViewGroup.refreshState_Words();
        preEditPopup.refreshState();
    }

    /**
     * ?????????????????????
     * */

    public void refreshDisplay_GoodsName(boolean special) {
        boolean isNK = Global.currentKeyboard == Global.KEYBOARD_T9 || Global.currentKeyboard == Global.KEYBOARD_SYM;
        boolean hideCandidate = Kernel.getWordsNumber() == 0 && !special;
        Kernel.setKernelType(isNK ? Kernel.NINE_KEY : Kernel.QWERT);

        if (mInputViewGG.isShown()) mInputViewGG.setVisibility(View.VISIBLE);
        functionViewGroup.refreshState(hideCandidate);
        functionsC.refreshStateForSecondLayout();
        functionsC.refreshStateForLargeCandidate(hideCandidate);
        if (lastHideState != hideCandidate && !hideCandidate) {
            viewSizeUpdate.UpdatePreEditSize();
            viewSizeUpdate.UpdateCandidateSize();
            viewSizeUpdate.UpdateLargeCandidateSize();
        }

        candidatesViewGroup.hide();
        candidatesViewGroup_words.hide();
        candidatesViewGroup_goodsName.refreshState(hideCandidate, isNK ? Global.EMOJI : Global.QUANPIN);
        candidatesViewGroup_goodsWords.hide();
        candidatesViewGroup_goodsInfo.hide();
        specialSymbolChooseViewGroup.refreshState(hideCandidate);
        prefixViewGroup.refreshState();
        t9InputViewGroup.refreshState();
        qkInputViewGroup.refreshState();
        quickSymbolViewGroup.refreshState();
        viewSizeUpdate.UpdateQuickSymbolSize();
        bottomBarViewGroup.refreshState();
        preEditPopup.refreshState();
    }

    /**
     * ????????????????????????????????????
     * */

    public void refreshDisplay_GoodsInfo(boolean special) {
        boolean isNK = Global.currentKeyboard == Global.KEYBOARD_T9 || Global.currentKeyboard == Global.KEYBOARD_SYM;
        boolean hideCandidate = Kernel.getWordsNumber() == 0 && !special;
        Kernel.setKernelType(isNK ? Kernel.NINE_KEY : Kernel.QWERT);

        if (mInputViewGG.isShown()) mInputViewGG.setVisibility(View.VISIBLE);
        functionViewGroup.refreshState(hideCandidate);
        functionsC.refreshStateForSecondLayout();
        functionsC.refreshStateForLargeCandidate(hideCandidate);
        if (lastHideState != hideCandidate && !hideCandidate) {
            viewSizeUpdate.UpdatePreEditSize();
            viewSizeUpdate.UpdateCandidateSize();
            viewSizeUpdate.UpdateLargeCandidateSize();
        }

        candidatesViewGroup.hide();
        candidatesViewGroup_words.hide();
        candidatesViewGroup_goodsName.hide();
        candidatesViewGroup_goodsWords.hide();
        candidatesViewGroup_goodsInfo.refreshState(hideCandidate, isNK ? Global.EMOJI : Global.QUANPIN);
        specialSymbolChooseViewGroup.refreshState(hideCandidate);
        prefixViewGroup.refreshState();
        t9InputViewGroup.refreshState();
        qkInputViewGroup.refreshState();
        quickSymbolViewGroup.refreshState();
        viewSizeUpdate.UpdateQuickSymbolSize();
        bottomBarViewGroup.refreshState_GoodsInfo();
        preEditPopup.refreshState();
    }

    /**
     * ????????????????????????????????????
     * */

    public void refreshDisplay_GoodsWords(boolean special) {
        boolean isNK = Global.currentKeyboard == Global.KEYBOARD_T9 || Global.currentKeyboard == Global.KEYBOARD_SYM;
        boolean hideCandidate = Kernel.getWordsNumber() == 0 && !special;
        Kernel.setKernelType(isNK ? Kernel.NINE_KEY : Kernel.QWERT);

        if (mInputViewGG.isShown()) mInputViewGG.setVisibility(View.VISIBLE);
        functionViewGroup.refreshState(hideCandidate);
        functionsC.refreshStateForSecondLayout();
        functionsC.refreshStateForLargeCandidate(hideCandidate);
        if (lastHideState != hideCandidate && !hideCandidate) {
            viewSizeUpdate.UpdatePreEditSize();
            viewSizeUpdate.UpdateCandidateSize();
            viewSizeUpdate.UpdateLargeCandidateSize();
        }

        candidatesViewGroup.hide();
        candidatesViewGroup_words.hide();
        candidatesViewGroup_goodsName.hide();
        candidatesViewGroup_goodsWords.refreshState(hideCandidate, isNK ? Global.EMOJI : Global.QUANPIN);
        candidatesViewGroup_goodsInfo.hide();
        specialSymbolChooseViewGroup.refreshState(hideCandidate);
        prefixViewGroup.refreshState();
        t9InputViewGroup.refreshState();
        qkInputViewGroup.refreshState();
        quickSymbolViewGroup.refreshState();
        viewSizeUpdate.UpdateQuickSymbolSize();
        bottomBarViewGroup.refreshState_GoodsWords();
        preEditPopup.refreshState();
    }



    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? todo?????????????????????????????????????????????????????????
     * ???????????????????????????????????????????????????
     */
    public void refreshDisplay() {
        refreshDisplay(false);
    }

    public void deleteLast() {
        if ((Kernel.getWordsNumber() == 0) || Global.currentKeyboard == Global.KEYBOARD_SYM) {
            this.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        } else {
            if (Global.currentKeyboard == Global.KEYBOARD_T9) {
                String pinyin = Kernel.getWordsShowPinyin();
                Global.addToRedo(pinyin.substring(pinyin.length() - 1));
                t9InputViewGroup.updateFirstKeyText();
                editPinyin("", true);
            } else if (Global.currentKeyboard == Global.KEYBOARD_QK) {
                if (mQKOrEmoji.equals(Global.QUANPIN)) {
                    String pinyin = Kernel.getWordsShowPinyin();
                    Global.addToRedo(pinyin.substring(pinyin.length() - 1));
                    editPinyin("", true);
                } else if (mQKOrEmoji.equals(Global.EMOJI)) {
                    Kernel.cleanKernel();
                    refreshDisplay(true);
                }
            } else if (Global.currentKeyboard == Global.KEYBOARD_EN) {
                Kernel.cleanKernel();
                refreshDisplay(true);
            }
        }
    }

    private static final int TEXT_MAX_LENGTH = 100;

    public void deleteAll() {
        if (Kernel.getWordsNumber() == 0 || Global.currentKeyboard == Global.KEYBOARD_SYM) {
            InputConnection ic = SoftKeyboard.this.getCurrentInputConnection();
            if (ic != null) {
                Global.redoTextForDeleteAll = ic.getTextBeforeCursor(TEXT_MAX_LENGTH, 0);
                ic.deleteSurroundingText(Integer.MAX_VALUE, 0);
            }
        } else {
            Global.redoTextForDeleteAll_preedit = Kernel.getWordsShowPinyin();
            Global.redoTextForDeleteAll = "";
        }
        Kernel.cleanKernel();
        if (Global.currentKeyboard == Global.KEYBOARD_T9 || Global.currentKeyboard == Global.KEYBOARD_SYM || Global.currentKeyboard == Global.KEYBOARD_NUM) {
            t9InputViewGroup.updateFirstKeyText();
            refreshDisplay();
        } else if (Global.currentKeyboard == Global.KEYBOARD_QK) {
            refreshDisplay(!mQKOrEmoji.equals(Global.QUANPIN));
        } else if (Global.currentKeyboard == Global.KEYBOARD_EN) {
            refreshDisplay();
        }
        pinyinProc.computeCursorPosition(getCurrentInputConnection());
        qkInputViewGroup.refreshQKKeyboardPredict();//??????????????????
    }

    public void chooseWord(int index) {
        String text = Kernel.getWordSelectedWord(index);
        refreshDisplay();
        if (Global.currentKeyboard == Global.KEYBOARD_T9) {
            t9InputViewGroup.updateFirstKeyText();
        }
        if (text != null) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                Global.addToRedo(text);
                ic.commitText(text, 1);
            }
        }
        qkInputViewGroup.refreshQKKeyboardPredict();//??????????????????
    }

    /**
     * ??????,??????????????????????????????????????????????????????
     *
     * @param text ????????????
     */
    public void commitText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (text != null && ic != null) {
            Global.addToRedo(text);
            ic.commitText(text, 1);
        }
        qkInputViewGroup.refreshQKKeyboardPredict();//??????????????????
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param msg pinyin
     */
    public void sendMsgToKernel(String msg) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SEND_TO_KERNEL, msg));
    }

    /**
     * ??????????????????????????????
     *
     * @param msg pinyin
     */
    public void sendMsgToQKKernel(String msg) {
        mHandler.sendMessage(mHandler.obtainMessage(QK_MSG_SEND_TO_KERNEL, msg));
    }

    @SuppressWarnings("deprecation")
    public void updateWindowManager() {
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        wm.updateViewLayout(keyboardLayout, keyboardParams);
    }

    public void switchKeyboardTo(int keyboard, boolean showAnim) {
        keyBoardSwitcher.switchKeyboard(keyboard, showAnim);
    }

    public void clearAnimation() {
        quickSymbolViewGroup.clearAnimation();
        t9InputViewGroup.clearAnimation();
        qkInputViewGroup.clearAnimation();
        functionViewGroup.clearAnimation();
        bottomBarViewGroup.clearAnimation();
        prefixViewGroup.clearAnimation();
        largeCandidateButton.clearAnimation();
        candidatesViewGroup.clearAnimation();
        candidatesViewGroup_words.clearAnimation();
        candidatesViewGroup_goodsName.clearAnimation();
        candidatesViewGroup_goodsWords.clearAnimation();
        candidatesViewGroup_goodsInfo.clearAnimation();
        lightViewManager.invisibleLightView();
        specialSymbolChooseViewGroup.clearAnimation();
    }

    public OnChangeListener mOnSizeChangeListener = new OnChangeListener() {

        public void onSizeChange(Rect keyboardRect) {
            keyboardWidth = keyboardRect.width();
            keyboardHeight = keyboardRect.height();

            keyboardParams.x = keyboardRect.left;
            keyboardParams.y = keyboardRect.top;
            keyboardParams.width = keyboardWidth;
            keyboardParams.height = keyboardHeight;

            standardHorizontalGapDistance = keyboardWidth * 2 / 100;
            standardVerticalGapDistance = keyboardHeight * 2 / 100;

            viewSizeUpdate.updateViewSizeAndPosition();
            updateWindowManager();
        }

        public void onFinishSetting() {
            screenInfoC.WriteKeyboardSizeInfoToSharedPreference();
            viewManagerC.removeSetKeyboardSizeView();
        }

        public void onPosChange(Rect keyboardRect) {
            keyboardParams.x = keyboardRect.left % mScreenWidth;
            keyboardParams.y = keyboardRect.top % mScreenHeight;
            viewSizeUpdate.UpdateLightSize();//???????????????????????????????????????????????????
            screenInfoC.WriteKeyboardSizeInfoToSharedPreference();
            updateWindowManager();
        }

        public void onResetSetting() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SoftKeyboard.this);
            Editor edit = sp.edit();
            edit.remove(KEYBOARD_HEIGHT_S + orientation);
            edit.remove(KEYBOARD_WIDTH_S + orientation);
            edit.remove(KEYBOARD_X_S + orientation);
            edit.remove(KEYBOARD_Y_S + orientation);

            edit.remove(FULL_WIDTH_S + orientation);
            edit.remove(FULL_WIDTH_X_S + orientation);
            edit.apply();
            screenInfoC.LoadKeyboardSizeInfoFromSharedPreference();

            viewSizeUpdate.updateViewSizeAndPosition();

            updateSetKeyboardSizeViewPos();
            mSetKeyboardSizeView.invalidate();
        }
    };

    /**
     * ??????????????????????????????????????????????????????SwitchKeyBoard**
     */
    private class KeyBoardSwitcherC {

        /*????????????*/
        private void hideKeyboard(int keyboard, boolean showAnim) {
            if (Global.isQK(Global.currentKeyboard)) {
                qkInputViewGroup.hide(showAnim);
            } else {
                if (keyboard == Global.KEYBOARD_NUM || keyboard == Global.KEYBOARD_SYM || keyboard == Global.KEYBOARD_T9) {
                    t9InputViewGroup.T9ToNum(showAnim);
                } else {
                    t9InputViewGroup.hideT9(showAnim);
                }
            }
        }

        /*????????????*/
        private void showKeyboard(int keyboard, boolean showAnim) {
            if (Global.isQK(keyboard)) {
                qkInputViewGroup.reloadPredictText(keyboard);
                qkInputViewGroup.show(showAnim);
            } else {
                t9InputViewGroup.show(showAnim);
            }
        }

        public void switchKeyboard(int keyboard) {
            switchKeyboard(keyboard, false);
        }

        /**
         * ?????????????????????
         * ?????????????????????????????????????????????????????????????????????????????????**
         *
         * @param keyboard ?????????????????????
         * @param showAnim ??????????????????
         */
        public void switchKeyboard(int keyboard, boolean showAnim) {
            Kernel.cleanKernel();

            if (Global.currentKeyboard != keyboard) {
                //Toast.makeText(SoftKeyboard.this, "" + keyboard, Toast.LENGTH_SHORT).show();
                hideKeyboard(keyboard, showAnim);
                showKeyboard(keyboard, showAnim);
                quickSymbolViewGroup.updateCurrentSymbolsAndSetTheContent(keyboard);
                Global.currentKeyboard = keyboard;
            }

            viewSizeUpdate.updateViewSizeAndPosition();
            refreshDisplay();
        }
    }

    /**
     * ???????????????????????????????????????
     */
    public class FunctionsC {

        /**
         * ?????????????????????????????????????????????????????????????????????????????????
         * ?????????????????????????????????????????????
         *
         * @param ei ???????????????
         * @return ?????????????????????
         */
        private boolean isToShowUrl(EditorInfo ei) {
            return ei != null && ((ei.inputType & EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT
                    && (EditorInfo.TYPE_MASK_VARIATION & ei.inputType) == EditorInfo.TYPE_TEXT_VARIATION_URI
                    || (ei.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) == EditorInfo.IME_ACTION_GO);
        }

        /**
         * ?????????????????????????????????????????????
         * ????????????????????????????????????
         *
         * @param ei ???????????????
         * @return ????????????????????????boolean
         */
        private boolean isToShowEmail(EditorInfo ei) {
            return ei != null && (ei.inputType & EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT &&
                    (EditorInfo.TYPE_MASK_VARIATION & ei.inputType) == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
        }

        /**
         * ????????????????????????????????????????????????
         * ????????????????????????????????????
         */
        public final void showDefaultSymbolSet() {
            final EditorInfo ei = SoftKeyboard.this.getCurrentInputEditorInfo();
            if (isToShowUrl(ei)) {
                sendMsgToKernel("'u");
            } else if (isToShowEmail(ei)) {
                sendMsgToKernel("'e");
            } else {
                sendMsgToKernel("'");
            }
        }

        public void refreshStateForSecondLayout() {
            if (Global.inLarge) {
                secondLayerLayout.setVisibility(View.GONE);
            } else {
                secondLayerLayout.setVisibility(View.VISIBLE);
            }
        }

        public void refreshStateForLargeCandidate(boolean show) {
            if (Global.inLarge || show) {
                largeCandidateButton.setVisibility(View.GONE);
            } else {
                largeCandidateButton.setVisibility(View.VISIBLE);
            }
            largeCandidateButton.getBackground().setAlpha(Global.getCurrentAlpha());
        }

        public void updateSkin(TextView v, int textColor, int backgroundColor) {
            v.setTextColor(textColor);
            v.setBackgroundColor(backgroundColor);
            v.getBackground().setAlpha(Global.getCurrentAlpha());
        }

        /**
         * ????????????????????????????????????????????????
         */
        private int getKeyboardType(EditorInfo pEditorInfo) {
            int keyboardType = Global.KEYBOARD_QK;
            // ?????????????????????,?????????????????????
            if (pEditorInfo != null) {
                switch (pEditorInfo.inputType & EditorInfo.TYPE_MASK_CLASS) {
                    case EditorInfo.TYPE_CLASS_NUMBER:
                    case EditorInfo.TYPE_CLASS_DATETIME:
                    case EditorInfo.TYPE_CLASS_PHONE:
                        keyboardType = Global.KEYBOARD_NUM;
                        break;
                    case EditorInfo.TYPE_CLASS_TEXT:
                        switch (EditorInfo.TYPE_MASK_VARIATION & pEditorInfo.inputType) {
                            case EditorInfo.TYPE_TEXT_VARIATION_URI:
                            case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
                            case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
                            case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                                keyboardType = Global.KEYBOARD_EN;
                                break;
                            default:
                                keyboardType = zhKeyboard;
                                break;
                        }
                        break;
                    default:// ???????????????????????????
                        keyboardType = zhKeyboard;
                        break;
                }
            }
            return keyboardType;
        }
    }

    /**
     * ??????listener
     */
    /*??????????????????*/
    private class Listeners {
        //todo: largeCandidate button should belonged to candidateViewGroups, needs to refactor
        OnTouchListener largeCandidateOnTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                transparencyHandle.handleAlpha(motionEvent.getAction());
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    candidatesViewGroup.displayCandidates();
                    candidatesViewGroup.largeTheCandidate();
                    candidatesViewGroup_words.displayCandidates();
                    candidatesViewGroup_words.largeTheCandidate();
                    candidatesViewGroup_goodsName.displayCandidates();
                    candidatesViewGroup_goodsName.largeTheCandidate();
                    candidatesViewGroup_goodsInfo.displayCandidates();
                    candidatesViewGroup_goodsInfo.largeTheCandidate();
                    candidatesViewGroup_goodsWords.displayCandidates();
                    candidatesViewGroup_goodsWords.largeTheCandidate();

                    Global.inLarge = true;
                    bottomBarViewGroup.intoReturnState();
                }
                keyboardTouchEffect.onTouchEffectWithAnim(view, motionEvent.getAction(),
                        skinInfoManager.skinData.backcolor_touchdown,
                        skinInfoManager.skinData.backcolor_quickSymbol
                );
                return false;
            }
        };
    }

    /**
     * ??????view????????????????????????????????????,?????????????????????????????????????????????
     */
    private class KeyBoardCreate {

        private void LoadResources() {
            Resources res = getResources();
            mTypeface = Typeface.createFromAsset(getAssets(), res.getString(R.string.font_file_path));// ?????????????????????
            mFuncKeyboardText = res.getStringArray(R.array.FUNC_KEYBOARD_TEXT);

            Global.shadowRadius = res.getInteger(R.integer.SHADOW_RADIUS);

            // ???????????????????????????onWindowShown????????????
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SoftKeyboard.this);
            Global.slideDeleteSwitch = sharedPreferences.getBoolean("SLIDE_DELETE_CHECK", false);
            Global.shadowSwitch = sharedPreferences.getBoolean("SHADOW_TEXT_CHECK", true);
            /*
             * ????????????????????????
             */
            int themeType = sharedPreferences.getInt("THEME_TYPE", 0);
            boolean isDiy = sharedPreferences.getBoolean("IS_DIY", false);
            if (isDiy) {
                skinInfoManager.loadConfigurationFromDIY(SoftKeyboard.this);
            } else {
                skinInfoManager.loadConfigurationFromXML(themeType, res);
            }
            Global.currentSkinType = themeType;
        }

        private void CreateCandidateView() {
            candidatesViewGroup.setSoftKeyboard(SoftKeyboard.this);
            candidatesViewGroup.create(SoftKeyboard.this);
            candidatesViewGroup.addThisToView(keyboardLayout);

            candidatesViewGroup_words.setSoftKeyboard(SoftKeyboard.this);
            candidatesViewGroup_words.create(SoftKeyboard.this);
            candidatesViewGroup_words.addThisToView(keyboardLayout);

            candidatesViewGroup_goodsName.setSoftKeyboard(SoftKeyboard.this);
            candidatesViewGroup_goodsName.create(SoftKeyboard.this);
            candidatesViewGroup_goodsName.addThisToView(keyboardLayout);

            candidatesViewGroup_goodsInfo.setSoftKeyboard(SoftKeyboard.this);
            candidatesViewGroup_goodsInfo.create(SoftKeyboard.this);
            candidatesViewGroup_goodsInfo.addThisToView(keyboardLayout);

            candidatesViewGroup_goodsWords.setSoftKeyboard(SoftKeyboard.this);
            candidatesViewGroup_goodsWords.create(SoftKeyboard.this);
            candidatesViewGroup_goodsWords.addThisToView(keyboardLayout);
        }

        private void CreatePreEditView() {
            preEditPopup.setSoftKeyboard(SoftKeyboard.this);
            preEditPopup.create(SoftKeyboard.this);
        }

        /**
         * ??????????????????????????????
         */
        private void CreateLightView() {
            lightViewManager.setSoftkeyboard(SoftKeyboard.this);
            lightViewManager.create(SoftKeyboard.this);
            lightViewManager.setTypeface(mTypeface);
        }

        private void CreateFuncKey() {
            functionViewGroup.setSoftKeyboard(SoftKeyboard.this);
            functionViewGroup.create(SoftKeyboard.this);
            functionViewGroup.setTypeface(mTypeface);
            functionViewGroup.addThisToView(keyboardLayout);
        }

        private void CreatePrefixView() {
            prefixViewGroup.setSoftKeyboard(SoftKeyboard.this);
            prefixViewGroup.create(SoftKeyboard.this);
            prefixViewGroup.addThisToView(secondLayerLayout);
        }

        private void CreateQuickSymbol() {
            quickSymbolViewGroup.setSoftKeyboard(SoftKeyboard.this);
            quickSymbolViewGroup.setTypeface(mTypeface);
            quickSymbolViewGroup.create(SoftKeyboard.this);
            quickSymbolViewGroup.updateCurrentSymbolsAndSetTheContent(Global.currentKeyboard);
            quickSymbolViewGroup.addThisToView(secondLayerLayout);
        }

        private void CreateSpecialSymbolChoose() {
            specialSymbolChooseViewGroup.setSoftKeyboard(SoftKeyboard.this);
            specialSymbolChooseViewGroup.create(SoftKeyboard.this);
            specialSymbolChooseViewGroup.setTypeface(mTypeface);
            specialSymbolChooseViewGroup.addThisToView(secondLayerLayout);
        }

        private void CreateSetKeyboardSizeView() {
            mSetKeyboardSizeView = new SetKeyboardSizeView(SoftKeyboard.this, mOnSizeChangeListener);
            mSetKeyboardSizeView.SetTypeface(mTypeface);
            mSetKeyboardSizeView.SetMovingIcon((String) functionViewGroup.buttonList.get(0).getText());
        }

        private void CreateBottomBarViewGroup() {
            bottomBarViewGroup.setSoftKeyboard(SoftKeyboard.this);
            bottomBarViewGroup.create(SoftKeyboard.this);
            bottomBarViewGroup.setTypeFace(mTypeface);
            bottomBarViewGroup.addThisToView(keyboardLayout);
        }

        private void CreateLargeButton() {
            largeCandidateButton = new QuickButton(SoftKeyboard.this);
            largeCandidateButton.setTextColor(skinInfoManager.skinData.textcolor_quickSymbol);
            largeCandidateButton.setBackgroundColor(skinInfoManager.skinData.backcolor_quickSymbol);
            largeCandidateButton.getBackground().setAlpha(Global.getCurrentAlpha());
            largeCandidateButton.setTypeface(mTypeface);
            largeCandidateButton.setText(res.getString(R.string.largecandidate));
            largeCandidateButton.setOnTouchListener(listeners.largeCandidateOnTouchListener);
            if (Global.shadowSwitch)
                largeCandidateButton.setShadowLayer(Global.shadowRadius, 0, 0, skinInfoManager.skinData.shadow);
            largeCandidateButton.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 0);
            largeCandidateButton.itsLayoutParams = params;

            secondLayerLayout.addView(largeCandidateButton, params);
        }

        private void CreateEnglishKeyboard() {
            qkInputViewGroup.setSoftKeyboard(SoftKeyboard.this);
            qkInputViewGroup.create(SoftKeyboard.this);
            qkInputViewGroup.setTypeface(mTypeface);
            qkInputViewGroup.addThisToView(mInputViewGG);
        }

        private void CreateT9Keyboard() {
            t9InputViewGroup.setSoftKeyboard(SoftKeyboard.this);
            t9InputViewGroup.create(SoftKeyboard.this);
            t9InputViewGroup.setTypeface(mTypeface);
            t9InputViewGroup.addThisToView(mInputViewGG);
        }

        private void createKeyboard() {
            if (keyboardLayout.getChildCount() < 1) {
                LoadResources();
                CreateFuncKey();
                CreatePreEditView();
                CreateCandidateView();
                keyboardLayout.addView(secondLayerLayout, secondParams);
                CreatePrefixView();
                CreateQuickSymbol();
                CreateSpecialSymbolChoose();
                CreateLargeButton();
                mInputViewGG = new LinearLayout(SoftKeyboard.this);
                mInputViewGG.setGravity(Gravity.CENTER_HORIZONTAL);
                mGGParams = new LinearLayout.LayoutParams(0, 0);
                CreateT9Keyboard();
                CreateEnglishKeyboard();
                keyboardLayout.addView(mInputViewGG, mGGParams);
                CreateBottomBarViewGroup();
                CreateLightView();
                CreateSetKeyboardSizeView();
            }
        }
    }

    /**
     * ????????????view????????????????????????????????????????????????
     */
    public class ViewSizeUpdateC {
        private final float TEXTSIZE_RATE_CANDIDATE = (float) 0.8;
        private final float TEXTSIZE_RATE_BOTTOM = (float) 0.8;
        private final float TEXTSIZE_RATE_FUNCTION = (float) 0.8;
        private final float TEXTSIZE_RATE_T9 = (float) 0.8;
        private final float TEXTSIZE_RATE_QUICKSYMBOL = (float) 0.8;
        private final int PREFIX_WIDTH_RATE = 44;
        private final double PREEDIT_HEIGHT_RATE = 0.5;
        private final int HOR_GAP_NUM = 2;
        private final int BUTTON_SHOW_NUM = 6;

        private int[] layerHeightRate;

        private void UpdatePrefixSize() {
            prefixViewGroup.setPosition(0, 0);
            if (Global.currentKeyboard == Global.KEYBOARD_NUM || Global.currentKeyboard == Global.KEYBOARD_SYM || Global.currentKeyboard == Global.KEYBOARD_T9) {
                prefixViewGroup.setSize(keyboardWidth * PREFIX_WIDTH_RATE / 100, keyboardHeight * layerHeightRate[1] / 100);
            } else {
                prefixViewGroup.setSize(keyboardWidth - standardHorizontalGapDistance, keyboardHeight * layerHeightRate[1] / 100);
            }
            prefixViewGroup.setButtonPadding(standardHorizontalGapDistance);
            prefixViewGroup.updateViewLayout();
        }

        private void UpdatePreEditSize() {
            //keyboardParams.width - HOR_GAP_NUM * standardHorizontalGapDistance,
            //                    (int) (PREEDIT_HEIGHT_RATE * layerHeightRate[0] * keyboardParams.height / 100),
            //                    standardHorizontalGapDistance
            preEditPopup.upadteSize(keyboardParams.width,
                    (int) (PREEDIT_HEIGHT_RATE * layerHeightRate[0] * keyboardParams.height / 100),
                    standardHorizontalGapDistance);
        }

        private void UpdateQKSize() {
            qkInputViewGroup.setPosition(0, 0);
            qkInputViewGroup.setSize(keyboardWidth, keyboardHeight * layerHeightRate[2] / 100, standardHorizontalGapDistance);
            qkInputViewGroup.updateViewLayout();
        }

        private void UpdateQuickSymbolSize() {
            int height = keyboardHeight * layerHeightRate[1] / 100;
            int width = keyboardWidth - HOR_GAP_NUM * standardHorizontalGapDistance;
            int buttonWidth = width / BUTTON_SHOW_NUM;
            quickSymbolViewGroup.setPosition(0, 0);
            quickSymbolViewGroup.setButtonPadding(standardHorizontalGapDistance);
            quickSymbolViewGroup.setButtonWidth(buttonWidth);
            float textSize = DisplayUtil.px2sp(SoftKeyboard.this, Math.min(buttonWidth, height) * TEXTSIZE_RATE_QUICKSYMBOL);
            quickSymbolViewGroup.setTextSize(textSize);
            if (t9InputViewGroup.deleteButton.isShown() && largeCandidateButton.isShown()) {
                quickSymbolViewGroup.setSize(keyboardWidth * PREFIX_WIDTH_RATE / 100, height);
            } else if (t9InputViewGroup.deleteButton.isShown() || largeCandidateButton.isShown()) {
                quickSymbolViewGroup.setSize(keyboardWidth * res.getInteger(R.integer.PREEDIT_WIDTH) / 100, height);
            } else {
                quickSymbolViewGroup.setSize(width, height);
            }
            quickSymbolViewGroup.updateViewLayout();
        }

        private void UpdateSpecialSymbolChooseSize() {
            int height = keyboardHeight * layerHeightRate[1] / 100;
            specialSymbolChooseViewGroup.updateSize(keyboardWidth * res.getInteger(R.integer.PREEDIT_WIDTH) / 100, height);
            specialSymbolChooseViewGroup.setPosition(0, 0);
            specialSymbolChooseViewGroup.setTextSize(DisplayUtil.px2sp(SoftKeyboard.this, TEXTSIZE_RATE_QUICKSYMBOL * height));
        }

        private void UpdateT9Size() {
            t9InputViewGroup.setSize(keyboardWidth, mGGParams.height, standardHorizontalGapDistance);
            t9InputViewGroup.deleteButton.getPaint().setTextSize(DisplayUtil.px2sp(SoftKeyboard.this,
                    Math.min(t9InputViewGroup.deleteButton.itsLayoutParams.width, layerHeightRate[1] * keyboardHeight / 100) * TEXTSIZE_RATE_T9
            ));
            keyboardLayout.updateViewLayout(mInputViewGG, mGGParams);
        }

        private void UpdateLightSize() {
            lightViewManager.setSize(keyboardWidth, keyboardHeight * layerHeightRate[2] / 100,
                    keyboardParams.x,
                    keyboardParams.y + keyboardHeight * (layerHeightRate[0] + layerHeightRate[1]) / 100 + 2 * standardVerticalGapDistance //??????y??????
            );
        }

        private void UpdateFunctionsSize() {
            int height = keyboardHeight * layerHeightRate[0] / 100;/*prefix???????????????*/
            int buttonwidth = keyboardWidth / 7 - standardHorizontalGapDistance * 8 / 7;
            functionViewGroup.updatesize(keyboardWidth, height);
            functionViewGroup.setButtonPadding(standardHorizontalGapDistance);
            functionViewGroup.setButtonWidth(buttonwidth);
            functionViewGroup.setTextSize(DisplayUtil.px2sp(SoftKeyboard.this, Math.min(buttonwidth, height) * TEXTSIZE_RATE_FUNCTION));
        }

        /*???????????????????????????*/
        public void UpdateCandidateSize() {
            candidatesViewGroup.setPosition(standardHorizontalGapDistance, 0);
            candidatesViewGroup.setSize(keyboardWidth - HOR_GAP_NUM * standardHorizontalGapDistance, keyboardHeight * layerHeightRate[0] / 100);
            candidatesViewGroup.updateViewLayout();

            candidatesViewGroup_words.setPosition(standardHorizontalGapDistance, 0);
            candidatesViewGroup_words.setSize(keyboardWidth - HOR_GAP_NUM * standardHorizontalGapDistance, keyboardHeight * layerHeightRate[0] / 100);
            candidatesViewGroup_words.updateViewLayout();

            candidatesViewGroup_goodsName.setPosition(standardHorizontalGapDistance, 0);
            candidatesViewGroup_goodsName.setSize(keyboardWidth - HOR_GAP_NUM * standardHorizontalGapDistance, keyboardHeight * layerHeightRate[0] / 100);
            candidatesViewGroup_goodsName.updateViewLayout();

            candidatesViewGroup_goodsInfo.setPosition(standardHorizontalGapDistance, 0);
            candidatesViewGroup_goodsInfo.setSize(keyboardWidth - HOR_GAP_NUM * standardHorizontalGapDistance, keyboardHeight * layerHeightRate[0] / 100);
            candidatesViewGroup_goodsInfo.updateViewLayout();

            candidatesViewGroup_goodsWords.setPosition(standardHorizontalGapDistance, 0);
            candidatesViewGroup_goodsWords.setSize(keyboardWidth - HOR_GAP_NUM * standardHorizontalGapDistance, keyboardHeight * layerHeightRate[0] / 100);
            candidatesViewGroup_goodsWords.updateViewLayout();
        }

        private void UpdateBottomBarSize() {
            bottomBarViewGroup.setPosition(0, standardVerticalGapDistance);
            bottomBarViewGroup.setSize(keyboardWidth - standardHorizontalGapDistance, ViewGroup.LayoutParams.MATCH_PARENT);
            bottomBarViewGroup.setButtonPadding(standardHorizontalGapDistance);
            if (Global.currentKeyboard == Global.KEYBOARD_NUM) {
                bottomBarViewGroup.setButtonWidthByRate(res.getIntArray(R.array.BOTTOMBAR_NUM_KEY_WIDTH));
            } else {
                bottomBarViewGroup.setButtonWidthByRate(res.getIntArray(R.array.BOTTOMBAR_KEY_WIDTH));
            }
            bottomBarViewGroup.setTextSize(DisplayUtil.px2sp(SoftKeyboard.this, ((keyboardHeight * layerHeightRate[3]) / 100) * TEXTSIZE_RATE_BOTTOM));
            bottomBarViewGroup.updateViewLayout();
        }

        private void UpdateLargeCandidateSize() {
            largeCandidateButton.itsLayoutParams.height = LayoutParams.MATCH_PARENT;
            largeCandidateButton.itsLayoutParams.width = keyboardWidth - 3 * standardHorizontalGapDistance - res.getInteger(R.integer.PREEDIT_WIDTH) * keyboardWidth / 100;
            ((LinearLayout.LayoutParams) largeCandidateButton.itsLayoutParams).leftMargin = standardHorizontalGapDistance;

            largeCandidateButton.getPaint().setTextSize(DisplayUtil.px2sp(SoftKeyboard.this,
                    Math.min(secondParams.height, (100 - res.getInteger(R.integer.PREEDIT_WIDTH)) * keyboardWidth / 100) * TEXTSIZE_RATE_CANDIDATE
            ));
        }

        void updateViewSizeAndPosition() {
            layerHeightRate = res.getIntArray(R.array.LAYER_HEIGHT);

            mGGParams.topMargin = standardVerticalGapDistance;
            mGGParams.height = keyboardHeight * layerHeightRate[2] / 100;
            mGGParams.width = keyboardWidth;

            secondParams.height = keyboardHeight * layerHeightRate[1] / 100;
            secondParams.topMargin = standardVerticalGapDistance;
            secondParams.leftMargin = standardHorizontalGapDistance;

            UpdateT9Size();
            UpdateCandidateSize();
            UpdatePreEditSize();
            UpdateQKSize();
            UpdatePrefixSize();
            UpdateFunctionsSize();
            UpdateLargeCandidateSize();
            UpdateQuickSymbolSize();
            UpdateSpecialSymbolChooseSize();
            UpdateBottomBarSize();
            UpdateLightSize();
        }
    }

    /**
     * ?????????????????????????????????????????????????????????
     */
    public class SkinUpdateC {
        /**
         * ???????????????????????????????????????
         * ?????????????????????????????????
         */
        public void updateSkin() {
            /*
             * ????????????????????????
             */
            int themeType = PreferenceManager.getDefaultSharedPreferences(SoftKeyboard.this).getInt("THEME_TYPE", 0);
            boolean isDiy = PreferenceManager.getDefaultSharedPreferences(SoftKeyboard.this).getBoolean("IS_DIY", false);
            if (isDiy) {
                skinInfoManager.loadConfigurationFromDIY(SoftKeyboard.this);
            } else if (themeType != Global.currentSkinType) {
                skinInfoManager.loadConfigurationFromXML(themeType, res);
                Global.currentSkinType = themeType;
            }

            candidatesViewGroup.updateSkin();
            candidatesViewGroup_words.updateSkin();
            candidatesViewGroup_goodsName.updateSkin();
            candidatesViewGroup_goodsWords.updateSkin();
            candidatesViewGroup_goodsInfo.updateSkin();
            t9InputViewGroup.updateSkin();
            preEditPopup.updateSkin();
            prefixViewGroup.updateSkin();
            specialSymbolChooseViewGroup.updateSkin();
            quickSymbolViewGroup.updateSkin();
            qkInputViewGroup.updateSkin();
            functionViewGroup.updateSkin();
            bottomBarViewGroup.updateSkin();
            functionsC.updateSkin(largeCandidateButton, skinInfoManager.skinData.textcolor_quickSymbol, skinInfoManager.skinData.backcolor_quickSymbol);
            /*if (keyboardLayout.getBackground() != null)
                keyboardLayout.getBackground().setAlpha((int) (Global.keyboardViewBackgroundAlpha * 255));
            else {
                keyboardLayout.setBackgroundResource(R.drawable.blank);

                keyboardLayout.setBackgroundColor(skinInfoManager.skinData.backcolor_touchdown);
                keyboardLayout.getBackground().setAlpha((int) (Global.keyboardViewBackgroundAlpha * 255));
            }*/
        }

        void updateShadowLayer() {
            candidatesViewGroup.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            candidatesViewGroup_words.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            candidatesViewGroup_goodsName.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            candidatesViewGroup_goodsWords.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            candidatesViewGroup_goodsInfo.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            prefixViewGroup.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            quickSymbolViewGroup.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            functionViewGroup.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            bottomBarViewGroup.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            specialSymbolChooseViewGroup.setShadowLayer(Global.shadowRadius, skinInfoManager.skinData.shadow);
            largeCandidateButton.setShadowLayer(Global.shadowRadius, 0, 0, skinInfoManager.skinData.shadow);
        }
    }

    /**
     * ?????????????????????
     */
    public class ScreenInfoC {

        private final float KEYBOARD_Y_RATE = (float) 0.33;
        private final float KEYBOARD_WIDTH_RATE = (float) 0.66;
        private final float KEYBOARD_HEIGHT_RATE = (float) 0.5;

        /**
         * ???????????????????????????
         * ????????????????????????????????????????????????????????????????????????????????????
         */
        private void refreshScreenInfo() {
            WindowManager wm = (WindowManager) getApplicationContext()
                    .getSystemService(WINDOW_SERVICE);
            Display dis = wm.getDefaultDisplay();
            mScreenWidth = dis.getWidth();
            mScreenHeight = dis.getHeight();
            orientation = mScreenWidth > mScreenHeight ? ORI_HOR : ORI_VER;
            mStatusBarHeight = getStatusBarHeight();

            DEFAULT_KEYBOARD_Y = (int) (mScreenHeight * KEYBOARD_Y_RATE);
            DEFAULT_KEYBOARD_WIDTH = (int) (mScreenWidth * KEYBOARD_WIDTH_RATE);
            DEFAULT_KEYBOARD_HEIGHT = (int) (mScreenHeight * KEYBOARD_HEIGHT_RATE);

            DEFAULT_FULL_WIDTH = mScreenWidth;
            DEFAULT_FULL_WIDTH_X = 0;
            DEFAULT_KEYBOARD_X = DEFAULT_KEYBOARD_WIDTH;
        }

        int getStatusBarHeight() {
            int result = 0;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        }

        /**
         * ????????????SharedPreference??????????????????????????????
         * ?????????????????????????????????????????????
         */
        private void LoadKeyboardSizeInfoFromSharedPreference() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SoftKeyboard.this);

            keyboardParams.x = sp.getInt(FULL_WIDTH_X_S + orientation, DEFAULT_FULL_WIDTH_X);
            keyboardParams.y = sp.getInt(KEYBOARD_Y_S + orientation, DEFAULT_KEYBOARD_Y);
            keyboardWidth = sp.getInt(FULL_WIDTH_S + orientation, DEFAULT_FULL_WIDTH);
            keyboardHeight = sp.getInt(KEYBOARD_HEIGHT_S + orientation, DEFAULT_KEYBOARD_HEIGHT);

            keyboardParams.width = keyboardWidth;
            keyboardParams.height = keyboardHeight;

            standardVerticalGapDistance = keyboardHeight * 2 / 100;
            standardHorizontalGapDistance = keyboardWidth * 2 / 100;
        }

        /**
         * ????????????????????????????????????SharedPreference
         * ?????????????????????????????????
         */
        public void WriteKeyboardSizeInfoToSharedPreference() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SoftKeyboard.this);
            Editor editor = sp.edit();

            editor.putInt(KEYBOARD_Y_S + orientation, keyboardParams.y);
            editor.putInt(KEYBOARD_HEIGHT_S + orientation, keyboardHeight);

            editor.putInt(FULL_WIDTH_X_S + orientation, keyboardParams.x);
            editor.putInt(FULL_WIDTH_S + orientation, keyboardWidth);
            editor.apply();
        }
    }

    /**
     * ??????????????????????????????????????????
     */
    public class TransparencyHandle {
        boolean isUpAlpha = false;
        private final float autoDownAlpha = (float) 0.1;
        private final float autoDownAlphaTop = (float) 1.0;

        private void startAutoDownAlpha() {
            mHandler.sendEmptyMessageDelayed(MSG_HIDE, 1000);
        }

        /**
         * author:purebluesong
         *
         * @param alpha ??????0???1????????????????????????????????????
         */
        public void setKeyBoardAlpha(int alpha) {
            t9InputViewGroup.setBackgroundAlpha(alpha);
            functionViewGroup.setBackgroundAlpha(alpha);
            bottomBarViewGroup.setBackgroundAlpha(alpha);
            candidatesViewGroup.setBackgroundAlpha(alpha);
            candidatesViewGroup_words.setBackgroundAlpha(alpha);
            candidatesViewGroup_goodsName.setBackgroundAlpha(alpha);
            candidatesViewGroup_goodsWords.setBackgroundAlpha(alpha);
            candidatesViewGroup_goodsInfo.setBackgroundAlpha(alpha);
            specialSymbolChooseViewGroup.setBackgroundAlpha(alpha);
            prefixViewGroup.setBackgroundAlpha(alpha);
            qkInputViewGroup.setBackgroundAlpha(alpha);
            quickSymbolViewGroup.setBackgroundAlpha(alpha);
            largeCandidateButton.getBackground().setAlpha(alpha);
            preEditPopup.setBackgroundAlpha(alpha);
        }

        /**
         * ????????????????????????????????????
         * ???????????????7????????????????????????
         */
        private void UpAlpha() {
            if (!mWindowShown) return;
            Animation anim = AnimationUtils.loadAnimation(SoftKeyboard.this, R.anim.hide);
            if (!Global.isQK(Global.currentKeyboard)) {
                t9InputViewGroup.clearAnimation();
                if (t9InputViewGroup.isShown()) t9InputViewGroup.startAnimation(anim);
            } else {
                qkInputViewGroup.clearAnimation();
                if (qkInputViewGroup.isShown()) qkInputViewGroup.startAnimation(anim);
            }
            bottomBarViewGroup.setButtonAlpha(autoDownAlpha);
//            if (bottomBarViewGroup.isShown())bottomBarViewGroup.show(anim);
            if (functionViewGroup.isShown()) {
                functionViewGroup.clearAnimation();
                functionViewGroup.startAnimation(anim);
            }
            if (specialSymbolChooseViewGroup.isShown()) {
                specialSymbolChooseViewGroup.clearAnimation();
                specialSymbolChooseViewGroup.startAnimation(anim);
            }
            if (quickSymbolViewGroup.isShown()) {
                quickSymbolViewGroup.clearAnimation();
                quickSymbolViewGroup.startAnimation(anim);
            }

            candidatesViewGroup.setButtonAlpha(autoDownAlpha);
            candidatesViewGroup_words.setButtonAlpha(autoDownAlpha);
            candidatesViewGroup_goodsName.setButtonAlpha(autoDownAlpha);
            candidatesViewGroup_goodsWords.setButtonAlpha(autoDownAlpha);
            candidatesViewGroup_goodsInfo.setButtonAlpha(autoDownAlpha);
            candidatesViewGroup_goodsInfo.setNameAlpha(autoDownAlpha);
            candidatesViewGroup_goodsInfo.setPriceAlpha(autoDownAlpha);
            candidatesViewGroup_goodsInfo.setDescAlpha(autoDownAlpha);
            candidatesViewGroup_goodsInfo.setImageAlpha(autoDownAlpha);
            candidatesViewGroup_goodsInfo.setBehindAlpha(autoDownAlpha);
            largeCandidateButton.setAlpha(autoDownAlpha);
//            preEditPopup.setButtonAlpha(autoDownAlpha);
            isUpAlpha = true;
        }

        /**
         * ????????????????????????
         * ?????????????????????????????????touch??????
         */
        public void DownAlpha() {
            Animation anim = AnimationUtils.loadAnimation(SoftKeyboard.this, R.anim.show);
            if (!Global.isQK(Global.currentKeyboard)) {
                if (t9InputViewGroup.isShown()) {
                    t9InputViewGroup.clearAnimation();
                    t9InputViewGroup.startAnimation(anim);
                }
            } else {
                if (qkInputViewGroup.isShown()) qkInputViewGroup.startAnimation(anim);
            }
            bottomBarViewGroup.setButtonAlpha(autoDownAlphaTop);
            if (functionViewGroup.isShown()) functionViewGroup.startAnimation(anim);
            if (specialSymbolChooseViewGroup.isShown())
                specialSymbolChooseViewGroup.startAnimation(anim);
            if (quickSymbolViewGroup.isShown()) quickSymbolViewGroup.startAnimation(anim);
            if (prefixViewGroup.isShown()) prefixViewGroup.startAnimation(anim);
//            preEditPopup.setButtonAlpha(autoDownAlphaTop);
            candidatesViewGroup.setButtonAlpha(autoDownAlphaTop);
            candidatesViewGroup_words.setButtonAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsName.setButtonAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsWords.setButtonAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsInfo.setButtonAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsInfo.setNameAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsInfo.setPriceAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsInfo.setDescAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsInfo.setImageAlpha(autoDownAlphaTop);
            candidatesViewGroup_goodsInfo.setBehindAlpha(autoDownAlphaTop);
            largeCandidateButton.setAlpha(autoDownAlphaTop);
            isUpAlpha = false;
        }

        public void handleAlpha(int eventAction) {
            Global.keyboardRestTimeCount = 0;
            if (eventAction == MotionEvent.ACTION_DOWN) {
                if (isUpAlpha) {
                    if (!mWindowShown) return;
                    DownAlpha();
                }
            }
        }

    }

    /**
     * keyboardLayout????????????????????????view??????????????????
     */
    public class ViewManagerC {
        /**
         */
        public void addInputView() {
            if (!keyboardLayout.isShown()) {
                WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
                screenInfoC.LoadKeyboardSizeInfoFromSharedPreference();

                keyboardParams.type = LayoutParams.TYPE_PHONE;
                keyboardParams.format = 1;
                keyboardParams.flags = DISABLE_LAYOUTPARAMS_FLAG;
                keyboardParams.gravity = Gravity.TOP | Gravity.LEFT;

                wm.addView(keyboardLayout, keyboardParams);
            }
        }

        /***/
        public void removeInputView() {
            if (null != keyboardLayout && keyboardLayout.isShown()) {
                WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
                wm.removeView(keyboardLayout);
            }
        }

        /**
         * ??????????????????????????????????????????
         * ???????????????touch RESIZE?????????
         *
         * @param type
         */
        public void addSetKeyboardSizeView(SettingType type) {
            mSetKeyboardSizeViewOn = true;//todo: what is this?
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);

            mSetKeyboardSizeParams.type = LayoutParams.TYPE_PHONE;
            mSetKeyboardSizeParams.format = 1;
            mSetKeyboardSizeParams.gravity = Gravity.TOP | Gravity.LEFT;
            mSetKeyboardSizeParams.x = 0;
            mSetKeyboardSizeParams.y = 0;
            mSetKeyboardSizeParams.width = mScreenWidth;
            mSetKeyboardSizeParams.height = mScreenHeight;

            mSetKeyboardSizeParams.flags = DISABLE_LAYOUTPARAMS_FLAG;
            updateSetKeyboardSizeViewPos();
            mSetKeyboardSizeView.SetSettingType(type);
            wm.addView(mSetKeyboardSizeView, mSetKeyboardSizeParams);
        }

        public void removeSetKeyboardSizeView() {
            if (mSetKeyboardSizeViewOn) {
                WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
                wm.removeView(mSetKeyboardSizeView);
            }
            mSetKeyboardSizeViewOn = false;
        }

    }

    /**
     * ????????????????????????
     */
    private void startShowAnimation() {
        if (!Global.isQK(Global.currentKeyboard)) {
            qkInputViewGroup.setVisibility(View.GONE);
            t9InputViewGroup.startShowAnimation();
        } else {
            t9InputViewGroup.setVisibility(View.GONE);
            qkInputViewGroup.show();
        }
        prefixViewGroup.setVisibility(View.GONE);
        quickSymbolViewGroup.setVisibility(View.VISIBLE);

        functionViewGroup.setVisibility(View.VISIBLE);
        functionViewGroup.startShowAnimation();

        bottomBarViewGroup.setVisibility(View.VISIBLE);
        bottomBarViewGroup.startShowAnimation();

        /*if (keyboardLayout.getBackground() != null) {
            keyboardLayout.getBackground().setAlpha((int) (Global.keyboardViewBackgroundAlpha * 255));
        }*/

        if (keyboardLayout.getBackground() != null)
            keyboardLayout.getBackground().setAlpha(255);
    }

    /**
     * ?????????????????????
     */
    private void startOutAnimation() {
        if (!Global.isQK(Global.currentKeyboard)) {
            qkInputViewGroup.setVisibility(View.GONE);
            t9InputViewGroup.startHideAnimation();
        } else {
            t9InputViewGroup.setVisibility(View.GONE);
            qkInputViewGroup.hide();
        }
        if (keyboardLayout.getBackground() != null)
            keyboardLayout.getBackground().setAlpha(0);

        if (largeCandidateButton.isShown()) {
            largeCandidateButton.setVisibility(View.GONE);
        }
        if (quickSymbolViewGroup.isShown()) {
            quickSymbolViewGroup.hide();
        }
        if (specialSymbolChooseViewGroup.isShown()) {
            specialSymbolChooseViewGroup.setVisibility(View.GONE);
        }
        prefixViewGroup.hide();
        functionViewGroup.startHideAnimation();
        bottomBarViewGroup.startHideAnimation();
        candidatesViewGroup.setVisibility(View.GONE);
        candidatesViewGroup_words.setVisibility(View.GONE);
        candidatesViewGroup_goodsName.setVisibility(View.GONE);
        candidatesViewGroup_goodsWords.setVisibility(View.GONE);
        candidatesViewGroup_goodsInfo.setVisibility(View.GONE);
        largeCandidateButton.setVisibility(View.GONE);

        lightViewManager.invisibleLightView();

    }

    @Override
    public View onCreateInputView() {
//        Log.d("WIVE","onCreateInputView");
        return super.onCreateInputView();
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     */
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
//        Log.d("WIVE","onStartInputView");
        initInputParam.initKernal(this.getApplicationContext());

        Global.inLarge = false;
        //??????enter_text
        bottomBarViewGroup.setEnterText(info, Global.currentKeyboard);
        /*
         * ????????????????????????
         */
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        zhKeyboard = sharedPreferences.getString("KEYBOARD_SELECTOR", "2").equals("1") ? Global.KEYBOARD_T9 : Global.KEYBOARD_QK;
        if (mWindowShown) {
            int keyboard = functionsC.getKeyboardType(info);
            keyBoardSwitcher.switchKeyboard(keyboard, true);
        }
        Global.keyboardRestTimeCount = 0;

        mHandler.removeMessages(MSG_DOUBLE_CLICK_REFRESH);
        mHandler.removeMessages(MSG_KERNEL_CLEAN);
        mHandler.sendEmptyMessageDelayed(MSG_DOUBLE_CLICK_REFRESH, 0);
        mHandler.sendEmptyMessageDelayed(MSG_KERNEL_CLEAN, maxFreeKernelTime * Global.metaRefreshTime);
        super.onStartInputView(info, restarting);
    }

    /**
     * ???????????????
     */
    @Override
    public void onWindowShown() {
//        Log.d("WIVE","onWindowShown");
        MobclickAgent.onResume(this);
        mWindowShown = true;
        mHandler.removeMessages(MSG_HIDE);
        mHandler.removeMessages(MSG_CLEAR_ANIMATION);
        mHandler.removeMessages(MSG_REMOVE_INPUT);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        keyboardTouchEffect.loadSetting(sp);
        Global.mCurrentAlpha = sp.getFloat("CURRENT_ALPHA", 1f);
        Global.shadowRadius = Integer.parseInt(sp.getString("SHADOW_TEXT_RADIUS", "5"));
        Global.slideDeleteSwitch = sp.getBoolean("SLIDE_DELETE_CHECK", true);

        viewManagerC.addInputView();
        lightViewManager.addToWindow();
        mInputViewGG.setVisibility(View.VISIBLE);

        try {
            quickSymbolViewGroup.updateSymbolsFromFile();
        } catch (IOException e) {
            CommonFuncs.showToast(this, "Sorry,There is an error when program load symbols from file");
        }

        EditorInfo info = this.getCurrentInputEditorInfo();
        keyBoardSwitcher.switchKeyboard(functionsC.getKeyboardType(info));

        keyboardParams.flags = ABLE_LAYOUTPARAMS_FLAG;
        if (keyboardLayout.isShown()) {
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
            wm.updateViewLayout(keyboardLayout, keyboardParams);
        }

        bottomBarViewGroup.spaceButton.setText(InputMode.halfToFull(sp.getString("ZH_SPACE_TEXT", "??????")));
        bottomBarViewGroup.companyWordsButton.setText(InputMode.halfToFull(sp.getString("ZH_SPACE_TEXT", "??????")));
        bottomBarViewGroup.userWordsButton.setText(InputMode.halfToFull(sp.getString("ZH_SPACE_TEXT", "??????")));
        bottomBarViewGroup.returnButton.setText(InputMode.halfToFull(sp.getString("ZH_SPACE_TEXT", "??????")));
        bottomBarViewGroup.backGoodsSelectButton.setText(InputMode.halfToFull(sp.getString("ZH_SPACE_TEXT", "??????")));
        if (sp.getBoolean("AUTO_DOWN_ALPHA_CHECK", true)) transparencyHandle.startAutoDownAlpha();

        keyboard_animation_switch = sp.getBoolean("KEYBOARD_ANIMATION", true);
        if (keyboard_animation_switch) {
            startShowAnimation();
        } else {
            keyBoardSwitcher.showKeyboard(functionsC.getKeyboardType(info), false);
        }
        skinUpdateC.updateSkin();
        skinUpdateC.updateShadowLayer();
        super.onWindowShown();
    }

    /**
     * ???????????????
     */
    @Override
    public void onWindowHidden() {
//        Log.d("WIVE","onWindowHidden");
        MobclickAgent.onPause(this);
        Kernel.cleanKernel();
        refreshDisplay();
        if (transparencyHandle.isUpAlpha) transparencyHandle.DownAlpha();
        clearAnimation();
        mWindowShown = false;
        t9InputViewGroup.updateFirstKeyText();
        if (keyboard_animation_switch) {
            startOutAnimation();
            mHandler.sendEmptyMessageDelayed(MSG_CLEAR_ANIMATION, DELAY_TIME_REMOVE);
            mHandler.sendEmptyMessageDelayed(MSG_REMOVE_INPUT, DELAY_TIME_REMOVE);
        } else {
            viewManagerC.removeInputView();
        }

        if (mSetKeyboardSizeViewOn) {
            mOnSizeChangeListener.onFinishSetting();
        }

        keyboardParams.flags = DISABLE_LAYOUTPARAMS_FLAG;
        if (keyboardLayout.isShown()) {
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
            wm.updateViewLayout(keyboardLayout, keyboardParams);
        }
        lightViewManager.removeView();
        mHandler.removeMessages(MSG_HIDE);
        super.onWindowHidden();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mSetKeyboardSizeViewOn) {
                mOnSizeChangeListener.onFinishSetting();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            char key = (char) ('a' + keyCode - KeyEvent.KEYCODE_A);
            Kernel.inputPinyin(key + "");
            refreshDisplay();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL && Kernel.getWordsNumber() > 0) {
            Kernel.deleteAction();
            refreshDisplay();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
