# Generated by Django 4.0.1 on 2022-01-27 23:01

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('rides', '0010_ridesharer_arrival_early_ridesharer_arrival_late_and_more'),
    ]

    operations = [
        migrations.AlterField(
            model_name='ridesharer',
            name='ride_order',
            field=models.ForeignKey(null=True, on_delete=django.db.models.deletion.CASCADE, to='rides.orderinfo'),
        ),
    ]
