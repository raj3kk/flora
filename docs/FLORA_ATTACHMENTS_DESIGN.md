# Chat attachments — design (v1.7, one-by-one agla step, journal PASS ke baad)

## Kya banega
- Chat me Attach button: user file chunega (GetContent).
- Image (jpg/png/webp) -> VisionClient se sawaal (user ki Gemini key,
  opt-in — user ne khud attach kiya hai).
- PDF/.md/.txt -> text nikaal kar (size cap 200 KB, pehle ~8000 chars)
  brain ko context ke saath bhejo.
- Koi file server par save/upload nahi hogi; content sirf us sawaal
  ke liye brain/vision ko jaayega.

## Audit points (implement karte waqt check karne hain)
- Launcher callback me use hone wale saare state vars callback se
  PEHLE declare hon (ConnectScreen wala order trap dobara nahi).
- File read fail ho to imaandari se "padh nahi payi" bolo, andaza jawab nahi.
- Bina Gemini key ke image attach par key-note dikhao, crash nahi.
- Talk-limit check attach wale brain call par bhi lagu (free cap).
- Koi nayi permission nahi (GetContent ko permission nahi chahiye).
