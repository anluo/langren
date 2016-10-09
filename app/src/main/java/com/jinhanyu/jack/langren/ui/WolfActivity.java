package com.jinhanyu.jack.langren.ui;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jinhanyu.jack.langren.ActionPerformer;
import com.jinhanyu.jack.langren.Constants;
import com.jinhanyu.jack.langren.MainApplication;
import com.jinhanyu.jack.langren.Me;
import com.jinhanyu.jack.langren.R;
import com.jinhanyu.jack.langren.TickTimer;
import com.jinhanyu.jack.langren.adapter.GameRoleCommonAdapter;
import com.jinhanyu.jack.langren.entity.GameRole;
import com.jinhanyu.jack.langren.entity.RoomInfo;
import com.jinhanyu.jack.langren.entity.VoteResult;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.emitter.Emitter;

public class WolfActivity extends CommonActivity implements ActionPerformer{
    private TextView tv_content;
    private EditText et_msg;
    private TextView time_label,action_done_label;
    private GameRoleCommonAdapter wolfAdapter;
    private ListView listView;
    private StringBuilder sb= new StringBuilder();

    private TickTimer tickTimer;

    private String toKillUserId;

    private ScrollView msg_scroll;

    @Override
    protected void prepareViews() {
        setContentView(R.layout.wolf);
        tv_content = (TextView) findViewById(R.id.tv_content);
        et_msg = (EditText) findViewById(R.id.et_msg);
        time_label = (TextView) findViewById(R.id.time_label);
        msg_scroll = (ScrollView) findViewById(R.id.msg_scroll);
        action_done_label = (TextView) findViewById(R.id.action_done_label);
        wolfAdapter = new GameRoleCommonAdapter(this,MainApplication.roomInfo.getUsers(), GameRole.Type.Wolf);
        listView = (ListView) findViewById(R.id.wolf_listView);
        listView.setAdapter(wolfAdapter);


        Toast.makeText(WolfActivity.this, "今晚你想刀谁？开始吧", Toast.LENGTH_SHORT).show();
        tickTimer = new TickTimer(time_label, Constants.WOLF_SECONDS,wolfAdapter){
            @Override
            protected void onTimeEnd() {
                super.onTimeEnd();
                MainApplication.socket.emit("wolf",MainApplication.roomInfo.getRoomId(), Me.getUserId(),toKillUserId);
            }
        };
        tickTimer.startTick();
    }

    @Override
    protected void prepareSocket() {
        MainApplication.socket.on("langrenMsg", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                 String userId= (String) args[0];
                 Log.i("userid",userId);
                 String msg = (String) args[1];
                 sb.append(MainApplication.roomInfo.findUserInRoom(userId).getNickname()).append(" 说: ").append(msg).append("\n");
                 tv_content.post(new Runnable() {
                     @Override
                     public void run() {
                         tv_content.setText(sb.toString());
                         msg_scroll.fullScroll(ScrollView.FOCUS_DOWN);//滚动到底部
                     }
                 });
            }
        }).on("wolfResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    String finalUserId = (String) args[0];
                    JSONArray array = (JSONArray) args[1];
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = (JSONObject) array.get(i);
                        String fromUserId = (String) obj.get("fromUserId");
                        String toUserId = (String) obj.get("toUserId");
                        MainApplication.roomInfo.getVoteResults().add(new VoteResult(MainApplication.roomInfo.findUserInRoom(fromUserId),MainApplication.roomInfo.findUserInRoom(toUserId)));
                    }
                    Intent intent =new Intent(WolfActivity.this, VoteResultActivity.class).putExtra("type",RoomInfo.VOTE_KILL);
                    if(finalUserId!=null)
                        intent.putExtra("finalUserName",MainApplication.roomInfo.findUserInRoom(finalUserId).getNickname());
                    startActivity(intent);
                    finish();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void unbindSocket() {
        MainApplication.socket.off("langrenMsg").off("wolfResult");
    }

    public void sendMsg(View view) {
        String msg = et_msg.getText().toString();
        if(TextUtils.isEmpty(msg))
            return;
        et_msg.setText("");
        MainApplication.socket.emit("langrenMsg",MainApplication.roomInfo.getRoomId(),Me.getUserId(),msg);
    }

    @Override
    public void doAction(Object... params) {
        toKillUserId = (String) params[0];
        action_done_label.setText("击杀完成，等待其他人...");
    }
}
