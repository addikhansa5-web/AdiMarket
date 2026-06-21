import os
import re

LAYOUT_DIR = 'app/src/main/res/layout'

# Regex to find a standard TextView header with a green background
header_pattern = re.compile(
    r'(<TextView\s+[^>]*?android:background="@color/primary_green"[^>]*?android:text="(@string/[^"]+)"[^>]*?>)',
    re.DOTALL
)

replacement_template = """    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="180dp">

        <ImageView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="centerCrop"
            android:src="@drawable/bg_vehicle_header" />

        <View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="@drawable/bg_image_overlay" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="24dp"
            android:text="{string_res}"
            android:textColor="#FFFFFF"
            android:textSize="26sp"
            android:textStyle="bold"
            app:layout_constraintBottom_toBottomOf="parent" />
    </androidx.constraintlayout.widget.ConstraintLayout>"""

count = 0
for filename in os.listdir(LAYOUT_DIR):
    if filename.endswith('.xml') and filename != 'activity_admin_approval.xml':
        filepath = os.path.join(LAYOUT_DIR, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        match = header_pattern.search(content)
        if match:
            string_res = match.group(2)
            new_header = replacement_template.format(string_res=string_res)
            
            # Need to ensure ConstraintLayout namespace is in the root tag if we add a ConstraintLayout inside
            # But actually ConstraintLayout doesn't need namespace inside if it doesn't use custom app: attributes,
            # wait, it uses `app:layout_constraintBottom_toBottomOf`.
            # So the root tag needs `xmlns:app="http://schemas.android.com/apk/res-auto"`.
            if 'xmlns:app' not in content:
                content = content.replace('xmlns:android="http://schemas.android.com/apk/res/android"',
                                          'xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:app="http://schemas.android.com/apk/res-auto"')
            
            new_content = content[:match.start()] + new_header + content[match.end():]
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated {filename}")
            count += 1

print(f"Done! Updated {count} files.")
