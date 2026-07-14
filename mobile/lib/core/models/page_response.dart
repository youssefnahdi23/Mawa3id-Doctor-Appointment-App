/// Spring's paged payload: `{content: [...], page: {size, number,
/// totalElements, totalPages}}` (PageSerializationMode.VIA_DTO).
class PageResponse<T> {
  const PageResponse({required this.content, required this.page});

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromItem,
  ) {
    return PageResponse(
      content: (json['content'] as List)
          .map((item) => fromItem(item as Map<String, dynamic>))
          .toList(),
      page: PageMeta.fromJson(json['page'] as Map<String, dynamic>),
    );
  }

  final List<T> content;
  final PageMeta page;

  bool get isLast => page.number + 1 >= page.totalPages;
}

class PageMeta {
  const PageMeta({
    required this.size,
    required this.number,
    required this.totalElements,
    required this.totalPages,
  });

  factory PageMeta.fromJson(Map<String, dynamic> json) => PageMeta(
        size: (json['size'] as num).toInt(),
        number: (json['number'] as num).toInt(),
        totalElements: (json['totalElements'] as num).toInt(),
        totalPages: (json['totalPages'] as num).toInt(),
      );

  /// Zero-based page index.
  final int number;
  final int size;
  final int totalElements;
  final int totalPages;
}
