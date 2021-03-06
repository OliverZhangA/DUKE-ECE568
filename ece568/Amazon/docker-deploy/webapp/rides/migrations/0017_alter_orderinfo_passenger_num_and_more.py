# Generated by Django 4.0.1 on 2022-01-31 05:35

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('rides', '0016_orderinfo_vehicle_capacity'),
    ]

    operations = [
        migrations.AlterField(
            model_name='orderinfo',
            name='passenger_num',
            field=models.IntegerField(default=0),
        ),
        migrations.AlterField(
            model_name='orderinfo',
            name='sharer_num',
            field=models.IntegerField(default=0),
        ),
        migrations.AlterField(
            model_name='orderinfo',
            name='vehicle_type',
            field=models.CharField(blank=True, choices=[('Sedan', 'Sedan'), ('Coupe', 'Gold'), ('SUV', 'SUV'), ('Minivan', 'Minivan')], max_length=30),
        ),
    ]
