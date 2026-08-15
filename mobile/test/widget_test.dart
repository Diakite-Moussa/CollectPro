import 'package:collectpro_mobile/main.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('CollectProApp smoke test', (WidgetTester tester) async {
    // Build CollectProApp wrapped in ProviderScope
    await tester.pumpWidget(
      const ProviderScope(
        child: CollectProApp(),
      ),
    );

    // Verify app builds without crashing
    expect(find.byType(CollectProApp), findsOneWidget);
  });
}
