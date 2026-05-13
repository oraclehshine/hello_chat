import 'package:app/core/models/upload_asset.dart';
import 'package:app/core/network/api_client.dart';
import 'package:dio/dio.dart';

class FileService {
  FileService(this._client);

  final ApiClient _client;

  Future<UploadAsset> uploadFile({
    required String filePath,
    required String fileName,
    required String scene,
  }) async {
    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(
        filePath,
        filename: fileName,
      ),
      'scene': scene,
    });
    final data = await _client.postForm('/files/upload', formData: formData);
    return UploadAsset.fromJson(data);
  }
}
