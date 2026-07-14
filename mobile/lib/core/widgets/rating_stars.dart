import 'package:flutter/material.dart';

/// Read-only 5-star average with the numeric count next to it.
/// Pass a null [count] to show the stars alone (e.g. a single review).
class RatingStars extends StatelessWidget {
  const RatingStars({
    super.key,
    required this.average,
    this.count,
    this.size = 16,
  });

  final double average;
  final int? count;
  final double size;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.tertiary;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (var star = 1; star <= 5; star++)
          Icon(
            average >= star
                ? Icons.star
                : average >= star - 0.5
                    ? Icons.star_half
                    : Icons.star_border,
            size: size,
            color: color,
          ),
        if (count != null) ...[
          const SizedBox(width: 4),
          Text(
            count == 0 ? '—' : '${average.toStringAsFixed(1)} ($count)',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ],
    );
  }
}
