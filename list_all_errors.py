from minio import Minio
import json
import sys

# 配置
ENDPOINT = "10.0.0.7:9000"
ACCESS_KEY = "MI1ypseFGQbWvaWDpVcG"
SECRET_KEY = "ZZcFkqcRXLOw6iGBkcut61n2aNReyQlyT7Nqvliq"
BUCKET_NAME = "wise-depot-error-logs"

def main():
    try:
        client = Minio(
            ENDPOINT,
            access_key=ACCESS_KEY,
            secret_key=SECRET_KEY,
            secure=False
        )

        if not client.bucket_exists(BUCKET_NAME):
            print(f"Bucket '{BUCKET_NAME}' 不存在。")
            return

        objects = list(client.list_objects(BUCKET_NAME))
        if not objects:
            print(f"Bucket '{BUCKET_NAME}' 中没有文件 (所有已知错误已修复并清理)。")
            return

        print(f"发现 {len(objects)} 个未处理的错误日志:")
        
        # 按最后修改时间排序
        objects.sort(key=lambda x: x.last_modified, reverse=True)
        
        for obj in objects:
            try:
                response = client.get_object(BUCKET_NAME, obj.object_name)
                content = response.read().decode('utf-8')
                response.close()
                response.release_conn()
                
                try:
                    data = json.loads(content)
                    exception = data.get('exceptionClass', 'UnknownException')
                    message = data.get('message', 'No message')
                    stack_trace = data.get('stackTrace', '')
                    # If message is null/empty, try to get first line of stack trace
                    if not message and stack_trace:
                        message = stack_trace.split('\n')[0]
                    
                    print(f"- [{obj.object_name}] {exception}: {message[:100]}...")
                except json.JSONDecodeError:
                    print(f"- [{obj.object_name}] (Invalid JSON content)")
            except Exception as e:
                print(f"- [{obj.object_name}] Error reading: {e}")

    except Exception as e:
        print(f"发生错误: {e}")

if __name__ == "__main__":
    main()
