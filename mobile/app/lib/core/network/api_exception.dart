class ApiException implements Exception {
  const ApiException(this.message, {this.code});

  final String message;
  final int? code;

  @override
  String toString() => message;
}
