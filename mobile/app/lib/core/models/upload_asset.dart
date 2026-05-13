class UploadAsset {
  const UploadAsset({
    required this.fileId,
    required this.fileUrl,
    required this.fileName,
    required this.fileType,
    required this.mimeType,
    required this.fileSize,
    required this.scene,
  });

  final int fileId;
  final String fileUrl;
  final String fileName;
  final String fileType;
  final String mimeType;
  final int fileSize;
  final String scene;

  factory UploadAsset.fromJson(Map<String, dynamic> json) {
    return UploadAsset(
      fileId: (json['fileId'] as num?)?.toInt() ?? 0,
      fileUrl: json['fileUrl']?.toString() ?? '',
      fileName: json['fileName']?.toString() ?? '',
      fileType: json['fileType']?.toString() ?? '',
      mimeType: json['mimeType']?.toString() ?? '',
      fileSize: (json['fileSize'] as num?)?.toInt() ?? 0,
      scene: json['scene']?.toString() ?? '',
    );
  }
}
