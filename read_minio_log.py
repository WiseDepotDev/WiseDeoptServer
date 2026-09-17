from minio import Minio
import json
import sys
from datetime import datetime

# 配置
ENDPOINT = "10.0.0.7:9000"
ACCESS_KEY = "MI1ypseFGQbWvaWDpVcG"
SECRET_KEY = "ZZcFkqcRXLOw6iGBkcut61n2aNReyQlyT7Nqvliq"
BUCKET_NAME = "wise-depot-error-logs"

def main():
    try:
        # 初始化客户端
        client = Minio(
            ENDPOINT,
            access_key=ACCESS_KEY,
            secret_key=SECRET_KEY,
            secure=False
        )

        # 检查 Bucket 是否存在
        if not client.bucket_exists(BUCKET_NAME):
            print(f"Bucket '{BUCKET_NAME}' 不存在。")
            return

        # 列出所有对象
        objects = list(client.list_objects(BUCKET_NAME))
        
        if not objects:
            print(f"Bucket '{BUCKET_NAME}' 中没有文件。")
            return

        # 按最后修改时间排序，取最新的
        objects.sort(key=lambda x: x.last_modified, reverse=True)
        latest_obj = objects[0]
        
        print(f"发现最新错误日志: {latest_obj.object_name}")
        print(f"时间: {latest_obj.last_modified}")
        print("-" * 50)

        # 读取内容
        response = client.get_object(BUCKET_NAME, latest_obj.object_name)
        try:
            content = response.read().decode('utf-8')
            # 尝试格式化 JSON
            try:
                json_content = json.loads(content)
                print(json.dumps(json_content, indent=2, ensure_ascii=False))
            except json.JSONDecodeError:
                print(content)
        finally:
            response.close()
            response.release_conn()

        # 交互式删除
        print("-" * 50)
        if sys.stdin.isatty():
            while True:
                choice = input(f"是否删除已解决的错误日志 {latest_obj.object_name}? (y/n): ").lower()
                if choice == 'y':
                    try:
                        client.remove_object(BUCKET_NAME, latest_obj.object_name)
                        print(f"文件 {latest_obj.object_name} 已成功删除。")
                    except Exception as e:
                        print(f"删除失败: {e}")
                    break
                elif choice == 'n':
                    print("文件保留。")
                    break
        else:
            print(f"提示: 在交互式终端运行此脚本可选择删除文件: {latest_obj.object_name}")

    except Exception as e:
        print(f"发生错误: {e}")

if __name__ == "__main__":
    main()
