import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 44});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      padding: EdgeInsets.all(size * 0.16),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.88),
        borderRadius: BorderRadius.circular(size * 0.32),
        border: Border.all(color: const Color(0xFFD8E7FF)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF2D7DFF).withValues(alpha: 0.10),
            blurRadius: size * 0.4,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: SvgPicture.asset(
        'assets/branding/dog.svg',
        fit: BoxFit.contain,
      ),
    );
  }
}
