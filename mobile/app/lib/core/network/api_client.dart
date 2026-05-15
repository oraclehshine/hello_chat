import 'dart:async';
import 'dart:io';

import 'package:app/core/network/api_exception.dart';
import 'package:app/core/storage/session_store.dart';
import 'package:dio/dio.dart';

class ApiClient {
  ApiClient({required this.baseUrl, required this.sessionStore}) {
    _dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        sendTimeout: const Duration(seconds: 30),
      ),
    );

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final token = sessionStore.accessToken;
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
        onError: (error, handler) {
          _logNetworkError(error);
          final apiError = _toApiException(error);
          if (_shouldForceLogout(
            error.response?.statusCode,
            apiError.code,
            apiError.message,
          )) {
            unawaited(sessionStore.clear());
          }
          handler.reject(
            DioException(
              requestOptions: error.requestOptions,
              error: apiError,
              response: error.response,
              type: error.type,
            ),
          );
        },
      ),
    );
  }

  final String baseUrl;
  final SessionStore sessionStore;
  late final Dio _dio;

  Future<Map<String, dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>(
      path,
      queryParameters: queryParameters,
    );
    return _unwrap(response.data);
  }

  Future<Map<String, dynamic>> post(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      path,
      data: data,
      queryParameters: queryParameters,
    );
    return _unwrap(response.data);
  }

  Future<Map<String, dynamic>> put(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) async {
    final response = await _dio.put<Map<String, dynamic>>(
      path,
      data: data,
      queryParameters: queryParameters,
    );
    return _unwrap(response.data);
  }

  Future<Map<String, dynamic>> delete(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) async {
    final response = await _dio.delete<Map<String, dynamic>>(
      path,
      data: data,
      queryParameters: queryParameters,
    );
    return _unwrap(response.data);
  }

  Future<Map<String, dynamic>> postForm(
    String path, {
    required FormData formData,
    Map<String, dynamic>? queryParameters,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      path,
      data: formData,
      queryParameters: queryParameters,
      options: Options(contentType: 'multipart/form-data'),
    );
    return _unwrap(response.data);
  }

  Map<String, dynamic> _unwrap(Map<String, dynamic>? body) {
    final payload = body ?? <String, dynamic>{};
    final code = (payload['code'] as num?)?.toInt() ?? 500;
    if (code != 0 && code != 200) {
      final message = payload['message']?.toString() ?? '请求失败';
      if (_shouldForceLogout(null, code, message)) {
        unawaited(sessionStore.clear());
      }
      throw ApiException(message, code: code);
    }
    final data = payload['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    return <String, dynamic>{'_value': data};
  }

  ApiException _toApiException(DioException error) {
    final statusCode = error.response?.statusCode;
    final data = error.response?.data;

    if (data is Map<String, dynamic>) {
      return ApiException(
        data['message']?.toString() ?? '请求失败',
        code: (data['code'] as num?)?.toInt() ?? statusCode,
      );
    }

    if (data is String && data.trim().isNotEmpty) {
      final msg = data.trim();
      if (_isTokenExpiredText(msg)) {
        return const ApiException('登录已过期，请重新登录', code: 401);
      }
      return ApiException(msg, code: statusCode);
    }

    if (statusCode == 401) {
      return const ApiException('登录已过期，请重新登录', code: 401);
    }

    return ApiException(error.message ?? '请求失败', code: statusCode);
  }

  void _logNetworkError(DioException error) {
    final request = error.requestOptions;
    final socketError = error.error is SocketException
        ? error.error as SocketException
        : null;
    final osError = socketError?.osError;
    // ignore: avoid_print
    print(
      '[ApiClient] request failed: '
      'method=${request.method}, '
      'url=${request.uri}, '
      'dioType=${error.type}, '
      'statusCode=${error.response?.statusCode}, '
      'socketMessage=${socketError?.message}, '
      'osCode=${osError?.errorCode}, '
      'osMessage=${osError?.message}',
    );
  }

  bool _shouldForceLogout(int? statusCode, int? code, String message) {
    if (statusCode == 401) return true;
    if (code == 401 || code == 40101 || code == 100401) return true;
    return _isTokenExpiredText(message);
  }

  bool _isTokenExpiredText(String message) {
    final lower = message.toLowerCase();
    return lower.contains('token expired') ||
        lower.contains('token invalid') ||
        message.contains('登录已过期');
  }
}
