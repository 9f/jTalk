/*
 * Copyright (C) 2012, Igor Ustyugov <igor@ustyugov.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/
 */

package net.ustyugov.jtalk.adapter;

import java.text.SimpleDateFormat;
import java.util.*;

import android.content.*;
import android.widget.*;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.Constants;
import net.ustyugov.jtalk.MessageItem;
import net.ustyugov.jtalk.service.JTalkService;
import net.ustyugov.jtalk.smiles.Smiles;
import net.ustyugov.jtalk.dialog.JuickMessageMenuDialog;
import net.ustyugov.jtalk.listener.TextLinkClickListener;
import net.ustyugov.jtalk.view.MyTextView;

import com.jtalk2.R;

import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

public class ChatAdapter extends ArrayAdapter<MessageItem> implements TextLinkClickListener {
    public enum ViewMode { single, multi }

	private String searchString = "";

	private SharedPreferences prefs;
	private Context context;
	private Smiles smiles;
	private String jid;
	private boolean firstClick = false;
	private boolean showtime;
	private Timer doubleClickTimer = new Timer();
    private ViewMode viewMode = ViewMode.single;
    private List<MessageItem> list = new ArrayList<MessageItem>();

	public ChatAdapter(Context context, Smiles smiles) {
        super(context, R.id.chat1);
        this.context = context;
        this.smiles  = smiles;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.showtime = prefs.getBoolean("ShowTime", false);
    }
	
	public void update(String account, String jid, String searchString, ViewMode viewMode) {
        if (this.viewMode == ViewMode.multi && viewMode == ViewMode.multi) return;

        this.viewMode = viewMode;
		this.jid = jid;
        this.searchString = searchString;
		clear();

        boolean showStatuses = prefs.getBoolean("ShowStatus", false);

        list = JTalkService.getInstance().getMessageList(account, jid);
        for (int i = 0; i < list.size(); i++) {
            MessageItem item = list.get(i);
            MessageItem.Type type = item.getType();
            if (searchString.length() > 0) {
                String name = item.getName();
                String body = item.getBody();
                String time = createTimeString(item.getTime());

                if (type == MessageItem.Type.status) {
                    if (showtime) body = time + "  " + body;
                } else {
                    if (showtime) body = time + " " + name + ": " + body;
                    else body = name + ": " + body;
                }

                if (body.toLowerCase().contains(searchString.toLowerCase())) {
                    add(item);
                }
            } else {
                if (showStatuses || type != MessageItem.Type.status) add(item);
            }
        }
	}
	
	public String getJid() { return this.jid; }

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		boolean enableCollapse = prefs.getBoolean("EnableCollapseMessages", true);
		int fontSize = Integer.parseInt(context.getResources().getString(R.string.DefaultFontSize));
		try {
			fontSize = Integer.parseInt(prefs.getString("FontSize", context.getResources().getString(R.string.DefaultFontSize)));
		} catch (NumberFormatException ignored) {	}
		
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.chat_item, null, false);
        }

        final MessageItem item = getItem(position);
        String subj = "";
        String body = item.getBody();
        String name = item.getName();
        MessageItem.Type type = item.getType();
        String nick = item.getName();
        final boolean collapsed = item.isCollapsed();
        boolean received = item.isReceived();
        String time = createTimeString(item.getTime());

        if (item.getSubject().length() > 0) subj = "\n" + context.getString(R.string.Subject) + ": " + item.getSubject() + "\n";
        body = subj + body;
        
        String message;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if (type == MessageItem.Type.status) {
        	if (showtime) message = time + "  " + body;
        	else message = body;
        	ssb.append(message);
        	ssb.setSpan(new ForegroundColorSpan(Colors.STATUS_MESSAGE), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
        	int colorLength = name.length();
        	int boldLength = colorLength;
        	
        	if (showtime) {
        		message = time + " " + name + ": " + body;
        		colorLength = name.length() + time.length() + 2;
        		boldLength = name.length() + time.length() + subj.length() + 2;
        	}
        	else message = name + ": " + body;
        	ssb.append(message);
        	ssb.setSpan(new ForegroundColorSpan(Colors.PRIMARY_TEXT), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, boldLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (!nick.equals(context.getResources().getString(R.string.Me)))
            	ssb.setSpan(new ForegroundColorSpan(Colors.INBOX_MESSAGE), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            else {
            	if (received) ssb.setSpan(new ForegroundColorSpan(Colors.OUTBOX_MESSAGE), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            	else ssb.setSpan(new ForegroundColorSpan(Colors.PRIMARY_TEXT), 0, colorLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            if (item.isEdited()) ssb.setSpan(new ForegroundColorSpan(Colors.HIGHLIGHT_TEXT), colorLength, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Search highlight
        if (searchString.length() > 0) {
            if (ssb.toString().toLowerCase().contains(searchString.toLowerCase())) {
                int from = 0;
                int start = -1;
                while ((start = ssb.toString().toLowerCase().indexOf(searchString.toLowerCase(), from)) != -1) {
                    from = start + searchString.length();
                    ssb.setSpan(new BackgroundColorSpan(Colors.SEARCH_BACKGROUND), start, start + searchString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        LinearLayout linear = (LinearLayout) convertView.findViewById(R.id.chat_item);
        linear.setMinimumHeight(Integer.parseInt(prefs.getString("SmilesSize", "24")));

        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.check);
        checkBox.setVisibility(viewMode == ViewMode.multi ? View.VISIBLE : View.GONE);
        checkBox.setChecked(item.isSelected());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                item.select(b);
                getItem(position).select(b);
            }
        });

        final ImageView expand = (ImageView) convertView.findViewById(R.id.expand);
        final MyTextView textView = (MyTextView) convertView.findViewById(R.id.chat1);
        textView.setOnTextLinkClickListener(this);
        if (enableCollapse) {
        	textView.setOnTouchListener(new OnTouchListener() {
            	View oldView = null;
    			public boolean onTouch(View view, MotionEvent event) {
    				switch (event.getAction()) {
    	    			case MotionEvent.ACTION_DOWN:
    	    				if (!firstClick) {
    	    					oldView = view;
    	    					firstClick = true;
    	    					doubleClickTimer.purge();
    	    					doubleClickTimer.cancel();
    	    					doubleClickTimer = new Timer();
    	    					doubleClickTimer.schedule(new TimerTask(){
    	    						@Override
    	    						public void run() {
    	    							firstClick = false;
    	    						}
    	    					}, 500);
    	    				} else {
    	    					firstClick = false;
    	    					if (oldView != null && oldView.equals(view)) {
    		    					if (item.isCollapsed()) {
    		    						item.setCollapsed(false);
    		    						textView.setSingleLine(false);
    		    						expand.setVisibility(View.GONE);
    		    					} else {
    		    						item.setCollapsed(true);
    		    						textView.setSingleLine(true);
    		    						expand.setVisibility(View.VISIBLE);
    		    					}
    	    					}
    	    				}
    	    				break;
    	    			default:
    	    				break;
    				}
    				return false;			
    			}
            });
        }
        
        if (collapsed && enableCollapse) {
        	textView.setSingleLine(true);
        	expand.setVisibility(View.VISIBLE);
        } else {
        	textView.setSingleLine(false);
        	expand.setVisibility(View.GONE);
        }
        
        if (prefs.getBoolean("ShowSmiles", true)) {
        	int startPosition = message.length() - body.length();
        	ssb = smiles.parseSmiles(textView, ssb, startPosition);
        }
        
        if (jid.equals(Constants.JUICK) || jid.equals(Constants.JUBO)) textView.setTextWithLinks(ssb, MyTextView.Mode.juick);
        else if (jid.equals(Constants.POINT)) textView.setTextWithLinks(ssb, MyTextView.Mode.point);
        else textView.setTextWithLinks(ssb);
        
        MovementMethod m = textView.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (textView.getLinksClickable()) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        
        textView.setTextSize(fontSize);

        convertView.setBackgroundColor(0X00000000);
        return convertView;
    }
	
	public void onTextLinkClick(View textView, String s) {
		if (s.length() > 1) {
			if (s.startsWith("@") || s.startsWith("#")) {
				new JuickMessageMenuDialog(context, s).show();
			} else {
                Uri uri = Uri.parse(s);
                if ((uri != null && uri.getScheme() != null) && (uri.getScheme().contains("http") || uri.getScheme().contains("xmpp"))) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    context.startActivity(intent);
                }
			}
		}
	}
	
    private String createTimeString(String time) {
        Date d = new Date();
        java.text.DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String currentDate = df.format(d).substring(0,10);
        if (currentDate.equals(time.substring(0,10))) return "(" + time.substring(11) + ")";
        else return "(" + time + ")";
    }

    public void copySelectedMessages() {
        String text = "";
        for(int i = 0; i < getCount(); i++) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean showtime = prefs.getBoolean("ShowTime", false);

            MessageItem message = getItem(i);
            if (message.isSelected()) {
                String body = message.getBody();
                String time = message.getTime();
                String name = message.getName();
                String t = "(" + time + ")";
                if (showtime) name = t + " " + name;
                text += "> " + name + ": " + body + "\n";
            }
        }

        ClipData.Item item = new ClipData.Item(text);

        String[] mimes = {"text/plain"};
        ClipData copyData = new ClipData(text, mimes, item);

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(copyData);
        Toast.makeText(context, R.string.MessagesCopied, Toast.LENGTH_SHORT).show();
    }
}
