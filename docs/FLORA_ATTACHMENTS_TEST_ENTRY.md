# Chat attachments (v1.7) — phone test entry (per-item audit gate)

Attachments audit ke teeno hisse me se (c) yahan taiyaar hai; (a) code
audit done hai, (b) CI result push ke baad aayega. Ye entry test plan
me merge hogi jab attachments audit PASS ho jayega.

## Phone par check karne wale steps
1. Chat me "Attach" dabao — file picker khulna chahiye.
2. Ek image chuno — "File attach ho gayi" note dikhna chahiye, phir
   sawaal likh kar Bhejo dabao — image ke baare me jawab aana chahiye
   (Gemini key Settings me honi chahiye; bina key ke key-note aana
   chahiye, crash nahi).
3. Ek text/.md file chuno + sawaal bhejo — file ke text ke aadhaar par
   jawab aana chahiye.
4. "Attach hatao" dabao — attach hatna chahiye, phir Bhejo sirf text
   sawaal ki tarah kaam karna chahiye.
5. PDF chuno — imaandari se "PDF abhi supported nahi" note aana
   chahiye, andaza jawab nahi.
6. Bina key ke image attach par key-note; free talk-limit khatam hone
   par limit note (donon me crash nahi).
