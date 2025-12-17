# convert_to_sql.py (인코딩 처리 강화 버전)

import csv
import re

# --- 설정 ---
input_filename = "korean_words.csv"
output_filename = "words.sql"
table_name = "dictionary"
column_name = "name"
target_pos = '명'
batch_size = 1000
# ------------

values = []
word_set = set()

print(f"'{input_filename}' CSV 파일을 읽어 '{output_filename}' 파일로 변환을 시작합니다...")

try:
    # [수정] CSV 읽기 시 'utf-8-sig' 사용 (BOM 제거), errors='replace' 추가
    with open(input_filename, 'r', encoding='utf-8-sig', errors='replace') as infile, \
            open(output_filename, 'w', encoding='utf-8') as outfile: # 쓰기는 'utf-8'

        reader = csv.reader(infile)
        try:
            next(reader) # 헤더 건너뛰기
        except StopIteration:
            print(f"오류: '{input_filename}' 파일이 비어있거나 헤더가 없습니다.")
            exit()

        # CREATE TABLE 구문 추가
        outfile.write(f"""CREATE TABLE IF NOT EXISTS {table_name} (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    {column_name} VARCHAR(100) NOT NULL UNIQUE,
    INDEX idx_name ({column_name})
) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;\n""") # [수정] Collation을 bin으로 명시

        row_count = 0
        valid_word_count = 0
        for row in reader:
            row_count += 1
            try:
                if len(row) < 3: # 최소 3개 컬럼 확인
                    print(f"경고: {row_count + 1}번째 줄 컬럼 부족, 건너뜁니다: {row}")
                    continue

                word_raw = row[1].strip()
                pos = row[2].strip()

                if target_pos and pos != target_pos:
                    continue

                word_cleaned = re.sub(r'\d+$', '', word_raw)
                word_lower = word_cleaned.lower().strip()

                if not word_lower or len(word_lower.encode('utf-8')) > 100 or word_lower in word_set: # 길이 체크를 바이트 단위로 변경
                    continue

                word_set.add(word_lower)
                word_escaped = word_cleaned.replace("'", "''")
                values.append(f"('{word_escaped}')")
                valid_word_count += 1

                if len(values) >= batch_size:
                    outfile.write(f"INSERT IGNORE INTO {table_name} ({column_name}) VALUES {','.join(values)};\n")
                    values = []

            except IndexError:
                print(f"경고: {row_count + 1}번째 줄 처리 중 오류 발생, 건너뜁니다: {row}")
            except Exception as e:
                print(f"경고: {row_count + 1}번째 줄 처리 중 예외 발생 ({e}), 건너뜁니다: {row}")


        if values:
            outfile.write(f"INSERT IGNORE INTO {table_name} ({column_name}) VALUES {','.join(values)};\n")

    print(f"변환 완료! (총 {valid_word_count}개의 고유한 '{target_pos}' 단어) '{output_filename}' 파일이 생성되었습니다.")
    print("이 파일을 'db-init' 폴더로 옮기고 도커를 재시작하세요.")

except FileNotFoundError:
    print(f"오류: '{input_filename}' 파일을 찾을 수 없습니다.")
except Exception as e:
    print(f"오류 발생: {e}")