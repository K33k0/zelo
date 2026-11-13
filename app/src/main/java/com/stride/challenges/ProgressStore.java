package com.stride.challenges;

import android.content.Context;
import org.json.*;
import java.time.LocalDate;
import java.util.UUID;

public final class ProgressStore {
    public static synchronized String read(Context context){return context.getSharedPreferences("zelo",0).getString("data",null);}
    public static synchronized int save(Context context,String text,int expected){
        try{
            String old=read(context);int revision=old==null?0:new JSONObject(old).optInt("revision",0);
            if(revision!=expected||text.length()>5_000_000)return -1;
            JSONObject data=new JSONObject(text);data.getJSONArray("challenges");data.put("version",2);data.put("revision",revision+1);
            if(!context.getSharedPreferences("zelo",0).edit().putString("data",data.toString()).commit())return -1;
            return revision+1;
        }catch(Exception e){return -1;}
    }
    public static JSONArray challenges(Context c){try{String raw=read(c);return raw==null?new JSONArray():new JSONObject(raw).getJSONArray("challenges");}catch(Exception e){return new JSONArray();}}
    public static synchronized boolean quickAdd(Context context,String id){
        try{
            JSONObject data=new JSONObject(read(context));JSONArray challenges=data.getJSONArray("challenges");
            for(int i=0;i<challenges.length();i++){
                JSONObject c=challenges.getJSONObject(i);if(!c.getString("id").equals(id))continue;
                GoalEngine.State s=GoalEngine.calculate(c,LocalDate.now());if(!s.scheduled||s.complete||s.due<=0)return false;
                double amount=GoalEngine.quickAmount(c);
                if(!Double.isFinite(amount)||amount<=0)return false;
                JSONObject log=new JSONObject().put("id",UUID.randomUUID().toString()).put("date",LocalDate.now().toString()).put("amount",amount).put("note","Home-screen quick add");
                c.getJSONArray("logs").put(log);
                if(save(context,data.toString(),data.optInt("revision",0))<0)return false;
                context.getSharedPreferences("zelo-widget-undo",0).edit().putString(id,log.toString()).apply();return true;
            }
        }catch(Exception e){android.util.Log.e("Zelo","Widget log failed",e);}return false;
    }
    public static synchronized JSONObject undoEntry(Context context,String id){
        try{String raw=context.getSharedPreferences("zelo-widget-undo",0).getString(id,null);if(raw==null)return null;JSONObject original=new JSONObject(raw);if(!LocalDate.now().toString().equals(original.getString("date")))return null;
            JSONArray all=challenges(context);for(int i=0;i<all.length();i++){JSONObject c=all.getJSONObject(i);if(!c.getString("id").equals(id))continue;JSONArray logs=c.getJSONArray("logs");for(int n=0;n<logs.length();n++){JSONObject l=logs.getJSONObject(n);if(l.getString("id").equals(original.getString("id"))&&l.getDouble("amount")==original.getDouble("amount")&&l.getString("date").equals(original.getString("date"))&&l.getString("note").equals(original.getString("note")))return l;}}
        }catch(Exception e){android.util.Log.w("Zelo","Undo no longer available",e);}return null;
    }
    public static synchronized boolean undoWidget(Context context,String id){
        try{JSONObject entry=undoEntry(context,id);if(entry==null)return false;JSONObject data=new JSONObject(read(context));JSONArray all=data.getJSONArray("challenges");for(int i=0;i<all.length();i++){JSONObject c=all.getJSONObject(i);if(!c.getString("id").equals(id))continue;JSONArray logs=c.getJSONArray("logs");for(int n=0;n<logs.length();n++)if(logs.getJSONObject(n).getString("id").equals(entry.getString("id"))){logs.remove(n);if(save(context,data.toString(),data.optInt("revision",0))<0)return false;context.getSharedPreferences("zelo-widget-undo",0).edit().remove(id).apply();return true;}}}catch(Exception e){android.util.Log.e("Zelo","Undo failed",e);}return false;
    }
    public static void changed(Context context){
        ReminderReceiver.refreshNotifications(context);
        ZeloWidget.updateAll(context);ReminderReceiver.scheduleAll(context);
        MainActivity.notifyDataChanged();
        BackupService.schedule(context,false);
    }
}
