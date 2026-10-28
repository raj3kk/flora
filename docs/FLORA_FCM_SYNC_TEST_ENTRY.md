# Flora v2.5 — FCM token server sync — phone test entry

Server side (pehle se live, function deploy ke baad):
- push-token-sync Edge Function (users upsert + push_tokens upsert)
- send-push Edge Function (admin JWT + is_admin check + server token lookup)

App side (v2.5):
- App khulte hi FCM token local save + server sync (LicenceClient.syncPushToken)
- Token refresh (onNewToken) par bhi server sync
- Sync fail ho to app rukta nahi; agle app-open par retry

Phone test steps:
1. v2.5 APK install karo, app kholo, FCM status line dekho (Settings).
2. Dashboard SQL se check karo (owner karega): push_tokens me is
   phone ki row aayi ya nahi (user_id + device_id + updated_at).
   Token VALUE kabhi chat/screenshot me mat bhejo.
3. Admin login banne ke baad: admin panel se trial_ending push
   bhejo, phone par notification aani chahiye.
4. Token refresh test: app data clear karke dobara kholo — nayi
   row/update honi chahiye, purani token se push nahi aana chahiye.

Known limits (imaandari se):
- push-token-sync me user JWT auth nahi hai (app me Auth session
  nahi hai); pehchaan random UUID user_id hai. Auth login aane
  par JWT check lagana hoga.
- Asli push ka test tabhi hoga jab admin user + is_admin flag banega.
- CI PASS sirf compile/build ka saboot hai, runtime ka nahi.
