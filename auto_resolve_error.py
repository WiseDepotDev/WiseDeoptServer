from minio import Minio
import json
import sys

# 配置
ENDPOINT = "10.0.0.7:9000"
ACCESS_KEY = "MI1ypseFGQbWvaWDpVcG"
SECRET_KEY = "ZZcFkqcRXLOw6iGBkcut61n2aNReyQlyT7Nqvliq"
BUCKET_NAME = "wise-depot-error-logs"

# 已知的已修复错误特征
FIXED_ERRORS = [
    "ConstraintViolationException",
    "user_profile_ibfk_1", 
    "Cannot delete or update a parent row",
    "IllegalArgumentException",
    "Name for argument of type",
    "-parameters",
    "DataIntegrityViolationException",
    "Duplicate entry",
    "user_role.uk_user_role",
    "VAL-0001",
    "currentPassword",
    "UserSecurity",
    "密码盐值不能为空",
    "角色名称已存在",
    "权限不存在",
    "角色不存在"
]

def main():
    try:
        # 初始化客户端
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
            print(f"Bucket '{BUCKET_NAME}' 中没有文件。")
            return

        # 按最后修改时间排序
        objects.sort(key=lambda x: x.last_modified, reverse=True)
        
        for obj in objects:
            print(f"检查文件: {obj.object_name}")
            response = client.get_object(BUCKET_NAME, obj.object_name)
            try:
                content = response.read().decode('utf-8')
                
                # 检查是否包含已修复的错误特征
                is_fixed = False
                
                # 错误1：用户删除外键约束
                if "ConstraintViolationException" in content and "user_profile" in content:
                    is_fixed = True
                    print("  -> 识别为已修复的 '用户删除外键约束' 错误。")
                
                # 错误2：缺少 -parameters 编译参数
                elif "IllegalArgumentException" in content and "-parameters" in content:
                    is_fixed = True
                    print("  -> 识别为已修复的 '缺少 -parameters 编译参数' 错误。")

                # 错误3：用户角色重复分配
                elif "DataIntegrityViolationException" in content and "Duplicate entry" in content:
                    is_fixed = True
                    print("  -> 识别为已修复的 '用户角色重复分配' 错误。")

                # 错误4：密码修改参数校验
                elif "VAL-0001" in content or "currentPassword" in content:
                    is_fixed = True
                    print("  -> 识别为已修复的 '密码修改参数校验' 错误。")

                # 错误5：用户安全信息缺少盐值
                elif "UserSecurity" in content and "密码盐值不能为空" in content:
                    is_fixed = True
                    print("  -> 识别为已修复的 '用户安全信息缺少盐值' 错误。")

                # 错误6：角色管理 RuntimeException
                elif "角色名称已存在" in content or "角色不存在" in content or "权限不存在" in content:
                    is_fixed = True
                    print("  -> 识别为已修复的 '角色管理 RuntimeException' 错误。")

                # 错误7：用户删除外键约束 (nfc_badge, key_access_audit_log, user_login_log)
                elif "DataIntegrityViolationException" in content and ("nfc_badge" in content or "key_access_audit_log" in content or "user_login_log" in content):
                    is_fixed = True
                    print("  -> 识别为已修复的 '用户删除外键约束 (nfc_badge, key_access_audit_log, user_login_log)' 错误。")
                
                if is_fixed:
                    print(f"  -> 删除文件: {obj.object_name}")
                    client.remove_object(BUCKET_NAME, obj.object_name)
                else:
                    print("  -> 未识别为已知已修复错误，保留文件。")
                    # 如果有未修复的错误，打印出来供人工检查（或者后续自动处理）
                    try:
                        json_content = json.loads(content)
                        print(f"  -> 错误摘要: {json_content.get('stackTrace', '')[:200]}...")
                    except:
                        pass

            finally:
                response.close()
                response.release_conn()

    except Exception as e:
        print(f"发生错误: {e}")

if __name__ == "__main__":
    main()
