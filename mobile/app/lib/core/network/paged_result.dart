class PagedResult<T> {
  const PagedResult({
    required this.list,
    required this.page,
    required this.pageSize,
    required this.total,
    required this.hasMore,
  });

  final List<T> list;
  final int page;
  final int pageSize;
  final int total;
  final bool hasMore;

  factory PagedResult.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    final rawList = (json['list'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(fromJson)
        .toList();

    return PagedResult<T>(
      list: rawList,
      page: (json['page'] as num?)?.toInt() ?? 1,
      pageSize: (json['pageSize'] as num?)?.toInt() ?? rawList.length,
      total: (json['total'] as num?)?.toInt() ?? rawList.length,
      hasMore: json['hasMore'] as bool? ?? false,
    );
  }
}
